/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.listTypeNameOf
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class BtaApiGenerator(
    private val targetPackage: String,
    private val skipXX: Boolean,
    private val kotlinVersion: KotlinReleaseVersion
) : BtaGenerator {
    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?): GeneratorOutputs {
        val className = level.name.capitalizeAsciiOnly()
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.builder(targetPackage, className).apply {
            addType(
                TypeSpec.interfaceBuilder(className).apply {
                    addKdoc(KDOC_SINCE_2_3_0)
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
                        maybeAddApplyArgumentStringsFun()
                    }
                    generateGetPutFunctions(argumentTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(level.transformApiArguments(), argumentTypeName)
                    }.build())
                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, className), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<BtaCompilerArgument>,
        argumentTypeName: ClassName,
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
                val enumConstants = type.java.enumConstants.filterIsInstance<Enum<*>>()
                enumsToGenerate[type] = generateEnumTypeBuilder(enumConstants, type.accessor())
                if (type !in enumsExperimental && experimental) {
                    enumsExperimental[type] = true
                } else if (type in enumsExperimental && !experimental) {
                    // if at least one option that is NOT experimental uses the enum
                    // then the enum is not experimental itself
                    enumsExperimental[type] = false
                }
                return ClassName("$targetPackage.enums", type.simpleName!!)
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
                    val argumentType =
                        argument.valueType.origin::class.supertypes.single { it.classifier == KotlinArgumentValueType::class }.arguments.first().type!!
                    argumentType.let {
                        when (val type = it.classifier) {
                            is KClass<*> if type.isSubclassOf(Enum::class) && type in enumNameAccessors -> {
                                generatedEnumType(type)
                            }
                            else -> {
                                it.asTypeName()
                            }
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

    fun generateEnumTypeBuilder(
        sourceEnum: Collection<Enum<*>>,
        nameAccessor: KProperty1<Any, String>,
    ): TypeSpec.Builder {
        val className = ClassName("$targetPackage.enums", sourceEnum.first()::class.simpleName!!)
        return TypeSpec.enumBuilder(className).apply {
            property<String>("stringValue") {
                initializer("stringValue")
            }
            addKdoc(KDOC_SINCE_2_3_0)
            primaryConstructor(FunSpec.constructorBuilder().addParameter("stringValue", String::class).build())
            sourceEnum.forEach {
                addEnumConstant(
                    it.name.uppercase(),
                    TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", nameAccessor.get(it)).build()
                )
            }
        }
    }

    fun writeEnumFile(typeSpec: TypeSpec, sourceEnum: KClass<*>) {
        val className = ClassName("$targetPackage.enums", sourceEnum.simpleName!!)
        val enumFileAppendable = createGeneratedFileAppendable()
        val enumFile = FileSpec.builder(className).apply {
            addType(typeSpec)
        }.build()
        enumFile.writeTo(enumFileAppendable)
        outputs += Path(enumFile.relativePath) to enumFileAppendable.toString()
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
        function("get") {
            addKdoc(KDOC_OPTIONS_GET)
            addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            returns(typeParameter)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
        }
        function("set") {
            addKdoc(KDOC_OPTIONS_SET)
            addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
        }

        function("contains") {
            addKdoc(KDOC_OPTIONS_CONTAINS)
            addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
            returns(BOOLEAN)
            addParameter("key", parameter.parameterizedBy(STAR))
        }
    }
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

private fun TypeSpec.Builder.maybeAddApplyArgumentStringsFun() {
    function("applyArgumentStrings") {
        addKdoc("Takes a list of string arguments in the format recognized by the Kotlin CLI compiler and applies the options parsed from them into this instance.")
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