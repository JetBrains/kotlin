/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.WithStringRepresentation
import org.jetbrains.kotlin.generators.kotlinpoet.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

internal class BtaApiOptionsGenerator(
    private val targetPackage: String,
    private val skipXX: Boolean,
    private val kotlinVersion: KotlinReleaseVersion,
) : BtaOptionsGenerator {
    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: ClassName?): GeneratorOutputs {
        val className = level.name.capitalizeAsciiOnly()
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.builder(targetPackage, className).apply {
            interfaceType(className) {
                addKdoc(levelsSince[level.name] ?: error("Level ${level.name} is missing in levelSince map"))
                if (level.name in experimentalLevelNames) {
                    addAnnotation(ANNOTATION_EXPERIMENTAL)
                }
                parentClass?.let { addSuperinterface(it) }
                val argument =
                    generateArgumentType(
                        className,
                        includeSinceVersion = true,
                        registerAsKnownArgument = false,
                        CodeBlock.of(KDOC_BASE_OPTIONS_CLASS, ClassName(targetPackage, className))
                    )
                val argumentTypeName = ClassName(targetPackage, className, argument)
                if (parentClass == null) {
                    addToArgumentStringsFun()
                }
                interfaceType("Builder") {
                    addKdoc("A builder for [$className].")
                    if (level.hadBuildersIntroducedLater) {
                        addKdoc("\n\n@since 2.3.20")
                    }
                    generateGetPutFunctions(argumentTypeName, level)
                    function("build") {
                        addKdoc("Constructs a new immutable [$className] instance with the options set in this builder.")
                        addModifiers(KModifier.ABSTRACT)
                        if (parentClass != null) {
                            addModifiers(KModifier.OVERRIDE)
                        }
                        if (!level.isLeaf()) {
                            addKdoc("\n\n")
                            addKdoc(KDOC_SINCE_2_4_20)
                        }
                        returns(ClassName(targetPackage, className))
                    }
                    if (parentClass == null) {
                        addApplyArgumentStringsFun()
                    } else {
                        addSuperinterface(parentClass.nestedClass("Builder"))
                    }
                }
                generateGetPutFunctions(argumentTypeName, level, deprecateSet = true)
                addType(TypeSpec.companionObjectBuilder().apply {
                    generateOptions(level.transformApiArguments(), argumentTypeName, level)
                }.build())
            }
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, className), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<BtaCompilerArgument<*>>,
        argumentTypeName: ClassName,
        level: KotlinCompilerArgumentsLevel,
    ) {
        val enumsToGenerate = mutableMapOf<KClass<*>, TypeSpec.Builder>()
        val enumsExperimental = mutableMapOf<KClass<*>, Boolean>()

        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach
            val experimental = name.startsWith("XX_") || name.startsWith("X_")

            /**
             * Marks enum to be generated and returns its name
             */
            fun generatedEnumType(type: KClass<*>): ClassName {
                require(WithStringRepresentation::class in type.allSuperclasses) {
                    "Compiler enum ${type.qualifiedName} must implement ${WithStringRepresentation::class.qualifiedName} to be used with BTA."
                }
                val enumConstants = type.java.enumConstants.filterIsInstance<Enum<*>>()
                @Suppress("UNCHECKED_CAST")
                enumConstants as List<WithStringRepresentation>
                enumsToGenerate[type] = generateEnumTypeBuilder(enumConstants, level)
                if (type !in enumsExperimental && experimental) {
                    enumsExperimental[type] = true
                } else if (type in enumsExperimental && !experimental) {
                    // if at least one option that is NOT experimental uses the enum
                    // then the enum is not experimental itself
                    enumsExperimental[type] = false
                }
                return type.toBtaEnumClassName()
            }

            // argument is newer than current version
            if (argument.introducedSinceVersion > kotlinVersion) {
                return@forEach
            }

            // argument was removed in or before current version - 3
            argument.removedSinceVersion?.let { removedVersion ->
                if (removedVersion <= getOldestSupportedVersion(kotlinVersion)) {
                    return@forEach
                }
            }

            val wasDeprecatedInVersion =
                argument.deprecatedSinceVersion?.takeIf { it <= kotlinVersion }

            // There's no need to generate any classes for custom representations as they're expected to already be there
            val argumentTypeParameter = when (argument.valueType) {
                is BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType -> {
                    val argumentType = argument.valueType.kType
                    val type = argumentType.classifier as? KClass<*> ?: error("Type is not a KClass: $argumentType")
                    when {
                        type.java.isEnum -> {
                            generatedEnumType(type)
                        }
                        else -> {
                            argumentType.asTypeName()
                        }
                    }
                }
                is BtaCompilerArgumentValueType.CustomArgumentValueType -> argument.valueType.type
            }.copy(nullable = argument.valueType.isNullable)

            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                annotation<JvmField>()
                // KT-28979 Need a way to escape /* in kdoc comments
                // inserting a zero-width space is not ideal, but we do actually have one compiler argument that breaks the KDoc without it
                addKdoc(argument.description.replace("/*", "/\u200B*").replace("*/", "*\u200B/"))
                maybeAddExperimentalAnnotation(experimental)
                maybeAddDeprecatedAnnotation(argument.removedSinceVersion, wasDeprecatedInVersion)

                val introducedVersion = argument.introducedSinceVersion
                initializer(
                    "%T(%S, %T(%L, %L, %L))",
                    argumentTypeName,
                    name,
                    kotlinVersionType,
                    introducedVersion.major,
                    introducedVersion.minor,
                    introducedVersion.patch,
                )
            }
        }

        enumsToGenerate.forEach { (type, typeSpecBuilder) ->
            if (enumsExperimental.getOrDefault(type, false)) {
                typeSpecBuilder.addAnnotation(ANNOTATION_EXPERIMENTAL)
            }
            writeEnumFile(typeSpecBuilder.build(), type)
        }
    }

    private fun PropertySpec.Builder.maybeAddExperimentalAnnotation(experimental: Boolean) {
        if (experimental) {
            addAnnotation(ANNOTATION_EXPERIMENTAL)
            addKdoc("\n\nWARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.")
        }
    }

    private fun PropertySpec.Builder.maybeAddDeprecatedAnnotation(
        removedVersion: KotlinReleaseVersion?,
        wasDeprecatedInVersion: KotlinReleaseVersion?,
    ) {
        if (removedVersion != null && removedVersion <= kotlinVersion) {
            annotation(ClassName(API_PACKAGE, "RemovedCompilerArgument")) {}
            if (wasDeprecatedInVersion != null) {
                addKdoc("\n\nDeprecated in Kotlin version ${wasDeprecatedInVersion.releaseName}.")
            }
            addKdoc("\n\nRemoved in Kotlin version ${removedVersion.releaseName}.")
        } else if (wasDeprecatedInVersion != null) {
            addKdoc("\n\nDeprecated in Kotlin version ${wasDeprecatedInVersion.releaseName}.")
            annotation(ClassName(API_PACKAGE, "DeprecatedCompilerArgument")) {}
        }
    }

    fun <T> generateEnumTypeBuilder(
        sourceEnum: Collection<T>,
        level: KotlinCompilerArgumentsLevel,
    ): TypeSpec.Builder where T : Enum<*>, T : WithStringRepresentation {
        val className = sourceEnum.first()::class.toBtaEnumClassName()
        return TypeSpec.enumBuilder(className).apply {
            property<String>("stringValue") {
                initializer("stringValue")
            }
            if (btaEnumVersionMap.contains(className)) {
                addKdoc("$KDOC_SINCE ${btaEnumVersionMap.getValue(className).releaseName}")
            } else {
                addKdoc(levelsSince[level.name] ?: error("Level ${level.name} is missing in levelSince map"))
            }
            primaryConstructor(FunSpec.constructorBuilder().addParameter("stringValue", String::class).build())
            val nameAccessor = WithStringRepresentation::stringRepresentation
            sourceEnum.forEach {
                addEnumConstant(
                    it.name.uppercase(),
                    TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", nameAccessor.get(it)).build()
                )
            }
        }
    }

    fun writeEnumFile(typeSpec: TypeSpec, sourceEnum: KClass<*>) {
        val className = sourceEnum.toBtaEnumClassName()
        val enumFileAppendable = createGeneratedFileAppendable()
        val enumFile = FileSpec.builder(className).apply {
            addType(typeSpec)
        }.build()
        enumFile.writeTo(enumFileAppendable)
        outputs += Path(enumFile.relativePath) to enumFileAppendable.toString()
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName, level: KotlinCompilerArgumentsLevel, deprecateSet: Boolean = false) {
        function("get") {
            addKdoc(KDOC_OPTIONS_GET)
            addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            returns(typeParameter)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
        }
        if (!deprecateSet) {
            function("set") {
                addKdoc(KDOC_OPTIONS_SET)
                addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
                val typeParameter = TypeVariableName("V")
                addTypeVariable(typeParameter)
                addParameter("key", parameter.parameterizedBy(typeParameter))
                addParameter("value", typeParameter)
            }
        }
        if (level.hadBuildersIntroducedLater) {
            withDeprecationCycle(
                kotlinVersion,
                warnFrom = KotlinReleaseVersion.v2_4_0,
                errorFrom = KotlinReleaseVersion.v2_5_0,
                removeFrom = KotlinReleaseVersion.v2_6_0,
                deprecationMessage = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments."
            ) { annotation ->
                function("contains") {
                    annotation?.let { addAnnotation(it) }
                    addKdoc(KDOC_OPTIONS_CONTAINS)
                    addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
                    returns(BOOLEAN)
                    addParameter("key", parameter.parameterizedBy(STAR))
                }
            }
        }
    }

    private val KotlinCompilerArgumentsLevel.hadBuildersIntroducedLater: Boolean get() = levelsSince[name] == KDOC_SINCE_2_3_0
}

internal fun TypeSpec.Builder.withDeprecationCycle(
    currentKotlinVersion: KotlinReleaseVersion,
    warnFrom: KotlinReleaseVersion? = null,
    errorFrom: KotlinReleaseVersion? = null,
    hideFrom: KotlinReleaseVersion? = null,
    removeFrom: KotlinReleaseVersion? = null,
    deprecationMessage: String,
    action: TypeSpec.Builder.(AnnotationSpec?) -> Unit,
) {
    val deprecationLevel = when {
        removeFrom != null && currentKotlinVersion >= removeFrom -> return
        hideFrom != null && currentKotlinVersion >= hideFrom -> DeprecationLevel.HIDDEN
        errorFrom != null && currentKotlinVersion >= errorFrom -> DeprecationLevel.ERROR
        warnFrom != null && currentKotlinVersion >= warnFrom -> DeprecationLevel.WARNING
        else -> null
    }
    val annotation = deprecationLevel?.let { deprecationLevel ->
        AnnotationSpec.builder(Deprecated::class.asTypeName()).apply {
            addMember(
                "message = %S",
                deprecationMessage
            )
            addMember("level = %T.%N", DeprecationLevel::class, deprecationLevel.name)
        }.build()
    }
    action(annotation)
}

internal fun TypeSpec.Builder.generateArgumentType(
    argumentsClassName: String,
    includeSinceVersion: Boolean,
    registerAsKnownArgument: Boolean,
    kDoc: CodeBlock? = null,
): String {
    require(argumentsClassName.endsWith("Arguments"))
    val argumentTypeName = argumentsClassName.removeSuffix("s")
    val typeSpec = TypeSpec.classBuilder(argumentTypeName).apply {
        kDoc?.let { addKdoc(it) }
        addTypeVariable(TypeVariableName("V"))
        property<String>("id") {
            initializer("id")
        }
        if (includeSinceVersion) {
            property("availableSinceVersion", kotlinVersionType) {
                initializer("availableSinceVersion")
            }
        }
        primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("id", String::class)
                .addParameterIf("availableSinceVersion", kotlinVersionType, includeSinceVersion)
                .build()
        )
        if (registerAsKnownArgument) {
            addInitializerBlock(CodeBlock.of("knownArguments.add(id)"))
        }
    }.build()
    addType(typeSpec)
    return argumentTypeName
}

private fun FunSpec.Builder.addParameterIf(name: String, type: ClassName, condition: Boolean): FunSpec.Builder {
    if (condition) {
        addParameter(name, type)
    }
    return this
}

private fun TypeSpec.Builder.addApplyArgumentStringsFun() {
    function("applyArgumentStrings") {
        addKdoc(
            """
        Takes a list of string arguments in the format recognized by the Kotlin CLI compiler and applies the options parsed from them into this instance.
        
        @throws org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException when the `arguments` contain errors and cannot be parsed
        """.trimIndent()
        )
        addParameter(
            ParameterSpec.builder("arguments", listTypeNameOf<String>())
                .addKdoc("a list of arguments for the Kotlin CLI compiler").build()
        )
        this.addModifiers(KModifier.ABSTRACT)
    }
}

private fun TypeSpec.Builder.addToArgumentStringsFun() {
    function("toArgumentStrings") {
        addKdoc("Converts the options to a list of string arguments recognized by the Kotlin CLI compiler.")
        returns(listTypeNameOf<String>())
        this.addModifiers(KModifier.ABSTRACT)
    }
}
