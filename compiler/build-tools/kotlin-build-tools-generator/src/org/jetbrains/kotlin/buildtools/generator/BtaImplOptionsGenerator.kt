/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.reflect.KClass

private const val ARGUMENT_PARSE_DIAGNOSTICS_CLASS = "ArgumentParseDiagnostics"

internal data class CompatLayerConfig(
    /**
     * The Kotlin version of the currently running build.
     */
    val currentKotlinVersion: KotlinReleaseVersion,
)

@OptIn(ExperimentalArgumentApi::class)
internal class BtaImplOptionsGenerator(
    override val targetPackage: String,
    private val skipXX: Boolean,
    /**
     * The Kotlin version that is used for generating arguments from SSoT.
     *
     * It's usually the Kotlin version of the currently running build,
     * but it will be set to an older version when generating the compat layer.
     */
    private val kotlinVersion: KotlinReleaseVersion,
    private val compatLayerConfig: CompatLayerConfig? = null,
) : BtaOptionsGenerator {

    private val generateCompatLayer = compatLayerConfig != null

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(
        level: KotlinCompilerArgumentsLevel,
        parentClass: ClassName?,
        additionalInterfaces: List<ClassName>,
    ): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val implClassName = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.builder(targetPackage, implClassName).apply {
            // Kotlinpoet requires these aliased imports when there's a name clash in the current context or else it calls the wrong member
            addAliasedImport(MemberName("org.jetbrains.kotlin.compilerRunner", "toArgumentStrings"), "compilerToArgumentStrings")
            addAliasedImport(MemberName(ClassName("org.jetbrains.kotlin.config", "KotlinCompilerVersion"), "VERSION"), "KC_VERSION")

            addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            classType(implClassName) {
                addModifiers(KModifier.INTERNAL)
                if (!level.isLeaf()) {
                    addModifiers(KModifier.ABSTRACT)
                }
                val syntheticInterfaces = syntheticArgumentInterfaces.filter { it.concreteClassName == implClassName }
                if (syntheticInterfaces.isEmpty()) {
                    addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()))
                    addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()).nestedClass("Builder"))
                } else {
                    syntheticInterfaces.forEach {
                        addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, it.name))
                        addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, it.name).nestedClass("Builder"))
                    }
                }

                if (parentClass != null) {
                    superclass(parentClass)
                    addSuperclassConstructorParameter("compilerArguments")
                    addSuperclassConstructorParameter("optionsMap")
                    if (!generateCompatLayer) {
                        addSuperclassConstructorParameter("argumentValidationErrors")
                        addSuperclassConstructorParameter("restrictedArgViolations")
                        addSuperclassConstructorParameter("argumentParseDiagnostics")
                    }
                }

                val toCompilerConverterFun = toCompilerArgumentsFunBuilder(level, parentClass)
                val toCompilerArgumentsAffectingOutcomeFun = toCompilerArgumentsAffectingOutcomeFunBuilder(level, parentClass)
                val applyCompilerArgumentsFun = applyCompilerArgumentsFunBuilder(level, parentClass)
                val defaultsInitializer = CodeBlock.builder()

                val argumentTypeNameString =
                    generateArgumentType(apiClassName, includeSinceVersion = false, registerAsKnownArgument = true)
                val argumentImplTypeName = ClassName(targetPackage, implClassName, argumentTypeNameString)
                val constructorSpecBuilder = constructorSpecBuilder(level)

                val mapProperty = generateOptionsMap(parentClass)
                generateCompilerArgumentsProperty(level, parentClass)

                generateOwnGetPutFunctions(argumentImplTypeName, mapProperty, level)

                if (syntheticInterfaces.isEmpty()) {
                    val argumentTypeName = ClassName(API_ARGUMENTS_PACKAGE, apiClassName, argumentTypeNameString)
                    generateGetPutFunctions(argumentTypeName, mapProperty, level)
                } else {
                    syntheticInterfaces.forEach { syntheticInterface ->
                        val argumentTypeName =
                            ClassName(API_ARGUMENTS_PACKAGE, syntheticInterface.name, syntheticInterface.name.removeSuffix("s"))
                        generateGetPutFunctions(argumentTypeName, mapProperty, level)
                    }
                }

                val getOptionBranches = mutableListOf<CodeBlock>()
                val setOptionBranches = mutableListOf<CodeBlock>()

                addType(TypeSpec.companionObjectBuilder().apply {
                    property(
                        "knownArguments",
                        ClassName("kotlin.collections", "MutableSet").parameterizedBy(ClassName("kotlin", "String")),
                        KModifier.PRIVATE
                    ) {
                        initializer("%M()", MemberName("kotlin.collections", "mutableSetOf"))
                    }
                    generateOptions(
                        arguments = level.transformImplArguments(),
                        implClassName = implClassName,
                        argumentTypeName = argumentImplTypeName,
                        applyCompilerArgumentsFun = applyCompilerArgumentsFun,
                        toCompilerConverterFun = toCompilerConverterFun,
                        toCompilerArgumentsAffectingOutcomeFun = toCompilerArgumentsAffectingOutcomeFun,
                        getOptionBranches = getOptionBranches,
                        setOptionBranches = setOptionBranches,
                    )
                }.build())

                generateOptionAccessorFunctions(getOptionBranches, setOptionBranches)


                // Initialize default values for custom arguments
                defaultsInitializer.build().takeIf { it.isNotEmpty() }?.let { addInitializerBlock(it) }

                if (level.isLeaf()) {
                    function("deepCopy") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(targetPackage, implClassName))
                        val copyCompilerArgs = generateCopyArgumentsBlock(
                            CodeBlock.of("this.compilerArguments"),
                            CodeBlock.of("%T()", level.getCompilerArgumentsClassName()),
                            level
                        )
                        val constructorArgs = if (!generateCompatLayer) {
                            CodeBlock.of(
                                "%L, optionsMap.toMutableMap(), _argumentValidationErrors.toMutableSet(), restrictedArgViolations.toList(),  argumentParseDiagnostics.copy()",
                                copyCompilerArgs
                            )
                        } else {
                            CodeBlock.of("%L, optionsMap", copyCompilerArgs)
                        }
                        addStatement(
                            "return %T($constructorArgs)",
                            ClassName(targetPackage, implClassName)
                        )
                    }
                    function("build") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(targetPackage, implClassName))
                        addStatement("return deepCopy()")
                    }
                    addSuperinterface(
                        ClassName(targetPackage.removeSuffix("arguments"), "DeepCopyable").parameterizedBy(
                            ClassName(targetPackage, implClassName)
                        )
                    )
                    if (!generateCompatLayer) {
                        toCompilerConverterFun.addStatement(
                            "%M(arguments)",
                            MemberName("org.jetbrains.kotlin.buildtools.internal.arguments", "populateExplicitArguments")
                        )
                    }
                } else {
                    function("build") {
                        addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        returns(ClassName(targetPackage, implClassName))
                    }
                }

                primaryConstructor(constructorSpecBuilder.build())

                toCompilerConverterFun.addStatement("return arguments")
                addFunction(toCompilerConverterFun.build())

                addFunction(applyCompilerArgumentsFun.build())

                addIsKnownArgumentFun(parentClass)

                if (!generateCompatLayer) {
                    toCompilerArgumentsAffectingOutcomeFun.addStatement("return arguments")
                    addFunction(toCompilerArgumentsAffectingOutcomeFun.build())
                }

                maybeAddApplyArgumentStringsFun(level, parentClass, generateCompatLayer)
                maybeAddToArgumentsStringFun(level, parentClass)
                if (!generateCompatLayer) {
                    generateRestrictedArgViolationCollection(level, parentClass)
                    generateToCompilationInputsFun(level)
                }
            }
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, implClassName), outputs)
    }

    private fun TypeSpec.Builder.addIsKnownArgumentFun(parentClass: ClassName?) {
        function("isArgumentKnown") {
            returns(Boolean::class)
            addParameter("name", String::class)
            addModifiers(KModifier.PROTECTED)
            if (parentClass != null) {
                addModifiers(KModifier.OVERRIDE)
                addStatement("return name in knownArguments || super.isArgumentKnown(name)")
            } else {
                addModifiers(KModifier.OPEN)
                addStatement("return name in knownArguments")
            }
        }
    }

    private fun constructorSpecBuilder(
        level: KotlinCompilerArgumentsLevel,
    ): FunSpec.Builder = FunSpec.constructorBuilder().apply {
        addParameter(ParameterSpec.builder("compilerArguments", level.getCompilerArgumentsClassName()).apply {
            if (level.isLeaf()) {
                defaultValue("%T()", level.getCompilerArgumentsClassName())
            }
        }.build())

        addParameter(
            ParameterSpec.builder(
                "optionsMap",
                ClassName("kotlin.collections", "MutableMap").parameterizedBy(
                    typeNameOf<String>(),
                    ANY.copy(nullable = true)
                ),
            ).apply {
                if (level.isLeaf()) {
                    defaultValue("%M()", MemberName("kotlin.collections", "mutableMapOf"))
                }
            }.build()
        )

        if (!generateCompatLayer) {
            addParameter(
                ParameterSpec.builder("argumentValidationErrors", setTypeNameOf<String>())
                    .defaultValue("%M()", MemberName("kotlin.collections", "emptySet"))
                    .build()
            )

            addParameter(
                ParameterSpec.builder(
                    "restrictedArgViolations",
                    ClassName("kotlin.collections", "List")
                        .parameterizedBy(
                            ClassName(targetPackage, "RestrictedArgViolation")
                        )
                )
                    .defaultValue("%M()", MemberName("kotlin.collections", "emptyList"))
                    .build()
            )

            addParameter(
                ParameterSpec.builder("argumentParseDiagnostics", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                    .defaultValue("%T()", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.generateOptionAccessorFunctions(
        getOptionBranches: List<CodeBlock>,
        setOptionBranches: List<CodeBlock>,
    ) {
        function("getOption") {
            addModifiers(KModifier.PRIVATE)
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
                addMember("%S", "DEPRECATION")
            }
            addParameter("keyId", String::class)
            returns(ANY.copy(nullable = true))
            val getOptionBody = CodeBlock.builder().apply {
                beginControlFlow("return when (keyId)")
                getOptionBranches.forEach { add(it) }
                beginControlFlow("else ->")
                addStatement("check(keyId in optionsMap) { \"Argument \${keyId} is not set and has no default value\" }")
                addStatement("optionsMap[keyId]")
                endControlFlow()
                endControlFlow()
            }.build()
            addCode(getOptionBody)
        }

        function("setOption") {
            addModifiers(KModifier.PRIVATE)
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
                addMember("%S", "DEPRECATION")
            }
            addParameter("keyId", String::class)
            addParameter("value", ANY.copy(nullable = true))
            val setOptionBody = CodeBlock.builder().apply {
                beginControlFlow("when (keyId)")
                setOptionBranches.forEach { add(it) }
                addStatement("else -> optionsMap[keyId] = value")
                endControlFlow()
            }.build()
            addCode(setOptionBody)
        }
    }


    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<BtaCompilerArgument<*>>,
        implClassName: String,
        argumentTypeName: ClassName,
        applyCompilerArgumentsFun: FunSpec.Builder,
        toCompilerConverterFun: FunSpec.Builder,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        getOptionBranches: MutableList<CodeBlock>,
        setOptionBranches: MutableList<CodeBlock>,
    ) {
        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach

            // argument is newer than currently generated version, skip it
            if (argument.introducedSinceVersion > kotlinVersion) {
                return@forEach
            }

            val wasRemoved = argument.removedSinceVersion?.let { removedVersion ->
                // argument was removed in or before current version - 3, skip it entirely
                if (removedVersion <= getOldestSupportedVersion(kotlinVersion)) {
                    return@forEach
                }
                true
            } ?: false

            // argument was introduced in one of recent versions, so it might not exist in older supported version
            val wasIntroducedRecently = (argument.introducedSinceVersion > getOldestSupportedVersion(kotlinVersion))

            // generate impl mirror of arguments
            val argumentTypeParameter = getArgumentTypeParameter(argument)

            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                initializer("%T(%S)", argumentTypeName, name)
            }
            when (argument) {
                is BtaCompilerArgument.SSoTCompilerArgument -> {
                    generateAutomaticArgumentsPropagators(
                        name,
                        argument,
                        wasRemoved,
                        argument.effectiveCompilerName,
                        toCompilerArgumentsAffectingOutcomeFun,
                        wasIntroducedRecently,
                        argumentTypeParameter,
                        getOptionBranches = getOptionBranches,
                        setOptionBranches = setOptionBranches,
                    )
                }

                is BtaCompilerArgument.CustomCompilerArgument -> {
                    generateCustomRepresentation(
                        implClassName,
                        name,
                        argument,
                        wasRemoved,
                        toCompilerConverterFun,
                        toCompilerArgumentsAffectingOutcomeFun,
                        applyCompilerArgumentsFun,
                        wasIntroducedRecently,
                        getOptionBranches = getOptionBranches,
                        setOptionBranches = setOptionBranches,
                    )
                }
            }
        }
    }

    private fun getArgumentTypeParameter(argument: BtaCompilerArgument<*>): TypeName = when (argument.valueType) {
        is BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType -> {
            val type = argument.valueType.kType
            val classifier = type.classifier as? KClass<*> ?: error("Type is not a KClass: $type")
            when {
                classifier.java.isEnum -> {
                    val classifier = type.classifier as KClass<*>
                    classifier.toBtaEnumClassName()
                }
                classifier == List::class && (type.arguments.first().type?.classifier as? KClass<*>)?.java?.isEnum == true -> {
                    listTypeNameOf((type.arguments.first().type?.classifier as KClass<*>).toBtaEnumClassName())
                }
                else -> {
                    type.asTypeName()
                }
            }
        }
        is BtaCompilerArgumentValueType.CustomArgumentValueType -> argument.valueType.type
    }.copy(nullable = argument.valueType.isNullable)

    private fun generateCustomRepresentation(
        implClassName: String,
        name: String,
        argument: BtaCompilerArgument.CustomCompilerArgument,
        wasRemoved: Boolean,
        toCompilerConverterFun: FunSpec.Builder,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        applyCompilerArgumentsFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
        getOptionBranches: MutableList<CodeBlock>,
        setOptionBranches: MutableList<CodeBlock>,
    ) {
        val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)
        val applier = MemberName(targetPackage, argument.applierSimpleName)

        argument.origin?.let { origin ->
            getOptionBranches += CodeBlock.builder().apply {
                add("%S -> {\n", name)
                if (wasIntroducedRecently) {
                    add("try {\n")
                    add("    %M(null, compilerArguments)\n", applier)
                    add("} catch (_: NoSuchMethodError) { null }\n")
                } else {
                    add("%M(null, compilerArguments)\n", applier)
                }
                add("}\n")
            }.build()
            setOptionBranches += CodeBlock.builder().apply {
                add("%S -> {\n", name)
                if (wasIntroducedRecently) {
                    add("try {\n")
                    add("    compilerArguments.%M(value as %T)", applier, argument.valueType.type)
                    add("} catch (_: NoSuchMethodError) { }\n")
                } else {
                    add("compilerArguments.%M(value as %T)", applier, argument.valueType.type)
                }
                add("}\n")
            }.build()
            val ssotArgument = BtaCompilerArgument.SSoTCompilerArgument(origin)
            addArgumentToAffectingOutcomeFun(
                ssotArgument,
                wasRemoved,
                ssotArgument.effectiveCompilerName,
                toCompilerArgumentsAffectingOutcomeFun,
                wasIntroducedRecently,
                name
            )
            return
        }

        // custom arguments that don't replace 1:1 an origin argument go here:

        getOptionBranches += CodeBlock.builder().apply {
            add("%S -> {\n", name)
            add("    if (%S in optionsMap) optionsMap[%S] else %L\n", name, name, argument.defaultValue)
            add("}\n")
        }.build()

        CodeBlock.builder().apply {
            add("if (%M in this) { ", member)
            add("arguments.%M(get(%M))", applier, member)
            add("}")
        }.build().also { setStatement ->
            toCompilerConverterFun.addStatement(
                "%L", generateSafeSetStatement(
                    wasIntroducedRecently,
                    wasRemoved,
                    name,
                    argument,
                    setStatement,
                    generateCompatLayer,
                )
            )
            if (argument.affectsCompilationOutcome) {
                toCompilerArgumentsAffectingOutcomeFun.addStatement(
                    "%L", generateSafeSetStatement(
                        wasIntroducedRecently,
                        wasRemoved,
                        name,
                        argument,
                        setStatement,
                        generateCompatLayer,
                    )
                )
            }
        }

        applyCompilerArgumentsFun.addStatement(
            "%L", generateSafeMethodAccessStatement(
                CodeBlock.builder().apply {
                    add("this[%M] = %M(if(%M in this) this[%M] else %L, arguments)", member, applier, member, member, argument.defaultValue)
                }.build(),
                catches =
                    buildList {
                        if (!generateCompatLayer) {
                            add(catchCompilerArgumentsParseException())
                        }
                        add(catchNoSuchMethodError())
                    },
            )
        )
    }

    /**
     * Generates code that configures for example [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] from [org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments] and vice versa
     */
    private fun generateAutomaticArgumentsPropagators(
        name: String,
        argument: BtaCompilerArgument.SSoTCompilerArgument,
        wasRemoved: Boolean,
        effectiveCompilerName: String,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
        argumentTypeParameter: TypeName,
        getOptionBranches: MutableList<CodeBlock>,
        setOptionBranches: MutableList<CodeBlock>,
    ) {

        val getTransform = buildCompilerToBtaGetStatement(
            argumentTypeParameter = argumentTypeParameter,
            argument = argument,
            effectiveCompilerName = effectiveCompilerName,
            wasRemoved = wasRemoved,
        )
        val getStatement = generateSafeSetStatement(
            wasIntroducedRecently,
            wasRemoved,
            name,
            argument,
            getTransform,
            generateCompatLayer,
        )
        getOptionBranches += CodeBlock.builder().apply {
            add("%S -> {\n", name)

            if (wasIntroducedRecently) {
                add("try {\n")
                add("%L\n", getStatement)
                add("} catch (_: NoSuchMethodError) { null }\n") // TODO : null???
            } else {
                add("%L\n", getStatement)
            }
            add("}\n")
        }.build()

        val compilerVal = buildBtaToCompilerSetStatement(
            argumentTypeParameter = argumentTypeParameter,
            argument = argument,
        )
        val setTransform = CodeBlock.builder().apply {
            if (wasRemoved) {
                add(
                    "this.compilerArguments.%M(%S, %L)\n",
                    MemberName(targetPackage, "setUsingReflection", isExtension = true),
                    effectiveCompilerName,
                    compilerVal
                )
            } else {
                add("this.compilerArguments.%N = %L\n", effectiveCompilerName, compilerVal)
            }
        }.build()
        val setStatement = generateSafeSetStatement(
            wasIntroducedRecently,
            wasRemoved,
            name,
            argument,
            setTransform,
            generateCompatLayer,
        )
        setOptionBranches += CodeBlock.builder().apply {
            add("%S -> {\n", name)
            if (wasIntroducedRecently) {
                add("try {\n")
                add("%L", setStatement)
                add("} catch (_: NoSuchMethodError) { }\n")
            } else {
                add("%L", setStatement)
            }
            add("}\n")
        }.build()

        addArgumentToAffectingOutcomeFun(
            argument,
            wasRemoved,
            effectiveCompilerName,
            toCompilerArgumentsAffectingOutcomeFun,
            wasIntroducedRecently,
            name
        )
    }

    private fun addArgumentToAffectingOutcomeFun(
        argument: BtaCompilerArgument.SSoTCompilerArgument,
        wasRemoved: Boolean,
        effectiveCompilerName: String,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
        name: String,
    ) {
        if (argument.affectsCompilationOutcome) {
            if (wasRemoved) {
                val setStatement = CodeBlock.of(
                    "try { arguments.%M(%S, this.compilerArguments.%M<%T>(%S)) } catch (_: %T) { }",
                    MemberName(targetPackage, "setUsingReflection", isExtension = true),
                    effectiveCompilerName,
                    MemberName(targetPackage, "getUsingReflection", isExtension = true),
                    argument.valueType.origin.getTypeArgumentForReflection(),
                    effectiveCompilerName,
                    NoSuchMethodError::class,
                )
                toCompilerArgumentsAffectingOutcomeFun.addStatement("%L", setStatement)
            } else {
                val setStatement = CodeBlock.of("arguments.%N = this.compilerArguments.%N", effectiveCompilerName, effectiveCompilerName)
                toCompilerArgumentsAffectingOutcomeFun.addStatement(
                    "%L", generateSafeSetStatement(
                        wasIntroducedRecently,
                        wasRemoved,
                        name,
                        argument,
                        setStatement,
                        generateCompatLayer,
                    )
                )
            }
        }
    }

    /**
     * Builds the value transformation from BTA to compiler (e.g., enum.stringValue, int.toString(), path.absolutePathStringOrThrow())
     */
    private fun buildBtaToCompilerSetStatement(
        argumentTypeParameter: TypeName,
        argument: BtaCompilerArgument.SSoTCompilerArgument,
    ): CodeBlock = CodeBlock.builder().apply {
        add("(value as %T)", argumentTypeParameter)

        when {
            argumentTypeParameter.isGeneratedEnum -> {
                add(maybeGetNullabilitySign(argument) + ".stringValue")
            }
            argumentTypeParameter.isGeneratedEnumList() -> {
                if (argument.valueType.isNullable) {
                    add("?.map { it.stringValue }?.toTypedArray() ?: emptyArray()")
                } else {
                    add(".map { it.stringValue }.toTypedArray()")
                }
            }
            argument.valueType.origin is IntType -> {
                add(maybeGetNullabilitySign(argument) + ".toString()")
            }
            argument.valueType.origin is PathType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(targetPackage, "absolutePathStringOrThrow", isExtension = true)
                )
            }
            argument.valueType.origin is StringArrayType -> {
                maybeAddDelimiterValidation(argument)
                if (argument.valueType.isNullable) {
                    add(" ?: emptyArray()")
                }
            }
            argument.valueType.origin is StringListType -> {
                maybeAddDelimiterValidation(argument)
                if (argument.valueType.isNullable) {
                    add(
                        "?.%M() ?: emptyArray()",
                        MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
                    )
                } else {
                    add(
                        ".%M()",
                        MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
                    )
                }
            }
            argument.valueType.origin is SearchPathType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M { it.%M() }",
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(targetPackage, "absolutePathStringOrThrow", isExtension = true),
                )
                maybeAddDelimiterValidation(argument)
                add(
                    maybeGetNullabilitySign(argument) + ".%M(%T.pathSeparator)",
                    MemberName(KOTLIN_COLLECTIONS, "joinToString"),
                    ClassName(JAVA_IO, "File")
                )
            }
            argument.valueType.origin is PathListType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M { it.%M() }",
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(targetPackage, "absolutePathStringOrThrow", isExtension = true),
                )
                maybeAddDelimiterValidation(argument)
                if (argument.valueType.isNullable) {
                    add(
                        "?.%M() ?: emptyArray()",
                        MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
                    )
                } else {
                    add(
                        ".%M()",
                        MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
                    )
                }
            }
            else -> add("")
        }
    }.build()

    /**
     * Builds the value transformation from compiler to BTA (e.g., string to enum, string.toInt(), path parsing)
     */
    private fun buildCompilerToBtaGetStatement(
        argumentTypeParameter: TypeName,
        argument: BtaCompilerArgument.SSoTCompilerArgument,
        effectiveCompilerName: String,
        wasRemoved: Boolean,
    ): CodeBlock = CodeBlock.builder().apply {
        val rawVal = CodeBlock.builder().apply {
            if (wasRemoved) {
                add(
                    "this.compilerArguments.%M<%T>(%S)",
                    MemberName(targetPackage, "getUsingReflection", isExtension = true),
                    argument.valueType.origin.getTypeArgumentForReflection(),
                    effectiveCompilerName
                )
            } else {
                add("this.compilerArguments.%N", effectiveCompilerName)
            }
        }.build()

        when {
            argumentTypeParameter.isGeneratedEnum -> {
                add("%L", rawVal)
                add(maybeGetNullabilitySign(argument))
                if (!generateCompatLayer) {
                    add(
                        $$".let { %T.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> %M(_restrictedArgViolations, this.compilerArguments::%N, entry.stringValue, it) } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                        argumentTypeParameter.copy(nullable = false),
                        MemberName(targetPackage, "checkCaseMatches"),
                        effectiveCompilerName,
                        MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
                    )
                } else {
                    add(
                        $$".let { %T.entries.firstOrNull { entry -> entry.stringValue.equals(it, true) } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                        argumentTypeParameter.copy(nullable = false),
                        MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
                    )
                }
            }
            argumentTypeParameter.isGeneratedEnumList() -> {
                val enumType = argumentTypeParameter.typeArguments[0]
                add("%L", rawVal)
                add(maybeGetNullabilitySign(argument))
                add(
                    $$".map { %T.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                    enumType.copy(nullable = false),
                    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException")
                )
            }
            argument.valueType.origin is IntType -> {
                add("%L", rawVal)
                add(maybeGetNullabilitySign(argument))
                add(".let { it.toInt() }")
            }
            argument.valueType.origin is PathType -> {
                add("%L", rawVal)
                add(maybeGetNullabilitySign(argument))
                add(".let { %M(it) }", MemberName(KOTLIN_IO_PATH, "Path"))
            }
            argument.valueType.origin is StringListType -> {
                add("%L", rawVal)
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(targetPackage, "toListOrEmpty", true)
                )
            }
            argument.valueType.origin is SearchPathType -> {
                add("%L", rawVal)
                add(
                    maybeGetNullabilitySign(argument) + ".%M(%T.pathSeparator)" + maybeGetNullabilitySign(argument) + ".%M { %M(it) }",
                    MemberName(KOTLIN_TEXT, "split", true),
                    ClassName(JAVA_IO, "File"),
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(KOTLIN_IO_PATH, "Path")
                )
            }
            argument.valueType.origin is PathListType -> {
                add("%L", rawVal)
                add(
                    maybeGetNullabilitySign(argument) + ".%M { %M(it) }",
                    MemberName(targetPackage, "mapOrEmpty", true),
                    MemberName(KOTLIN_IO_PATH, "Path")
                )
            }
            else -> add("%L", rawVal)
        }
    }.build()

    /**
     * The type of the property, used as the explicit type argument for `getUsingReflection` when an argument was removed from the compiler
     * and is accessed reflectively. It's needed for chained transforms, otherwise the type argument cannot be inferred.
     * See `Main.generateProperty` in the CLI arguments generator for similar code.
     */
    private fun KotlinArgumentValueType<*>.getTypeArgumentForReflection(): TypeName {
        val stringType = ClassName("kotlin", "String")
        return when (this) {
            is StringArrayType, is StringListType, is PathListType, is EnumListType<*> ->
                ClassName("kotlin", "Array").parameterizedBy(stringType)
            is BooleanType -> ClassName("kotlin", "Boolean").copy(nullable = isNullable.current)
            is SearchPathType -> stringType.copy(nullable = true)
            else -> stringType.copy(nullable = isNullable.current)
        }
    }

    private fun TypeSpec.Builder.generateToCompilationInputsFun(
        level: KotlinCompilerArgumentsLevel,
    ) {
        if (!level.isLeaf()) return
        function("toCompilationInputs") {
            addKdoc(
                """
                Returns a sorted list of compiler argument strings representing only the arguments
                that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
                Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
                only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
                skips properties whose value matches the default.
                """.trimIndent()
            )
            returns(listTypeNameOf<String>())
            addStatement("return toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()")
        }
    }

    private fun TypeSpec.Builder.generateOptionsMap(parentClass: ClassName?): PropertySpec = property(
        "optionsMap",
        ClassName("kotlin.collections", "MutableMap").parameterizedBy(typeNameOf<String>(), ANY.copy(nullable = true)),
        KModifier.PROTECTED
    ) {
        if (parentClass == null) {
            addModifiers(KModifier.OPEN)
        } else {
            addModifiers(KModifier.OVERRIDE)
        }
        initializer("optionsMap")
    }


    private fun TypeSpec.Builder.generateCompilerArgumentsProperty(
        level: KotlinCompilerArgumentsLevel,
        parentClass: ClassName?,
    ): PropertySpec =
        property(
            "compilerArguments",
            level.getCompilerArgumentsClassName(),
            KModifier.PROTECTED,
        ) {
            if (parentClass == null) {
                addModifiers(KModifier.OPEN)
            } else {
                addModifiers(KModifier.OVERRIDE)
            }
            initializer("compilerArguments")
        }

    fun TypeSpec.Builder.generateOwnGetPutFunctions(
        implParameter: ClassName,
        mapProperty: PropertySpec,
        level: KotlinCompilerArgumentsLevel,
    ) {
        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addStatement("return getOption(key.id) as %T", typeParameter)
        }
        function("set") {
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OPERATOR, KModifier.PRIVATE)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("setOption(key.id, value)")
        }

        function("contains") {
            addModifiers(KModifier.OPERATOR)
            returns(BOOLEAN)
            addParameter("key", implParameter.parameterizedBy(STAR))
            addStatement("return isArgumentKnown(key.id) ") // basically since all arguments have defaults...
        }
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName, mapProperty: PropertySpec, level: KotlinCompilerArgumentsLevel) {
        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            if (targetPackage == IMPL_ARGUMENTS_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            }
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement("return getOption(key.id) as %T", typeParameter)
        }
        function("set") {
            if (targetPackage == IMPL_ARGUMENTS_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            }
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)

            val currentKotlinVersion = if (generateCompatLayer) {
                addStatement(
                    "val currentKotlinVersion = %T(KC_VERSION)",
                    ClassName("org.jetbrains.kotlin.tooling.core", "KotlinToolingVersion")
                )
                CodeBlock.of("(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)")
            } else {
                CodeBlock.of(
                    "(%L, %L, %L)",
                    kotlinVersion.major,
                    kotlinVersion.minor,
                    kotlinVersion.patch
                )
            }
            addCode(
                CodeBlock.builder()
                    .beginControlFlow(
                        "if (key.availableSinceVersion > %T%L)",
                        kotlinVersionType,
                        currentKotlinVersion
                    )
                    .addStatement(
                        $$"throw %T(\"${key.id} is available only since ${key.availableSinceVersion}\")",
                        IllegalStateException::class
                    )
                    .endControlFlow()
                    .build()
            )
            addStatement("setOption(key.id, value)")
        }

        if (levelsSince[level.name] == KDOC_SINCE_2_3_0) {
            withDeprecationCycle(
                compatLayerConfig?.currentKotlinVersion ?: kotlinVersion,
                warnFrom = KotlinReleaseVersion.v2_4_0,
                errorFrom = KotlinReleaseVersion.v2_5_0,
                removeFrom = KotlinReleaseVersion.v2_6_0,
                deprecationMessage = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments."
            ) { annotation ->
                function("contains") {
                    annotation?.let { addAnnotation(it) }
                    addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
                    returns(BOOLEAN)
                    addParameter("key", parameter.parameterizedBy(STAR))
                    addStatement("return key.id in optionsMap")
                }
            }
        }
    }

    private fun TypeSpec.Builder.generateRestrictedArgViolationCollection(
        level: KotlinCompilerArgumentsLevel,
        parentClass: ClassName?,
    ) {
        val restrictedArgViolationClass = ClassName(targetPackage, "RestrictedArgViolation")
        val rootCompilerArgsClass = kotlinCompilerArguments.topLevel.getCompilerArgumentsClassName()
        val ownViolationInfos = collectRestrictedArgInfo(level)

        if (parentClass == null) {
            property(
                "_restrictedArgViolations",
                ClassName("kotlin.collections", "MutableList").parameterizedBy(restrictedArgViolationClass),
                KModifier.PROTECTED,
            ) {
                initializer(
                    "restrictedArgViolations.%M()",
                    MemberName("kotlin.collections", "toMutableList"),
                )
            }
            addProperty(
                PropertySpec.builder(
                    "restrictedArgViolations",
                    ClassName("kotlin.collections", "List").parameterizedBy(restrictedArgViolationClass),
                )
                    .addModifiers(KModifier.INTERNAL)
                    .getter(FunSpec.getterBuilder().addStatement("return _restrictedArgViolations").build())
                    .build()
            )
            property(
                "_argumentValidationErrors",
                ClassName("kotlin.collections", "MutableSet").parameterizedBy(typeNameOf<String>()),
                KModifier.PROTECTED,
            ) {
                initializer(
                    "argumentValidationErrors.%M()",
                    MemberName("kotlin.collections", "toMutableSet"),
                )
            }
            addProperty(
                PropertySpec.builder(
                    "argumentValidationErrors",
                    ClassName("kotlin.collections", "Set").parameterizedBy(typeNameOf<String>()),
                )
                    .addModifiers(KModifier.INTERNAL)
                    .getter(FunSpec.getterBuilder().addStatement("return _argumentValidationErrors").build())
                    .build()
            )
            addProperty(
                PropertySpec.builder("argumentParseDiagnostics", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                    .addModifiers(KModifier.INTERNAL)
                    .initializer("argumentParseDiagnostics")
                    .build()
            )
            function("collectRestrictedArgViolations") {
                addModifiers(KModifier.INTERNAL, KModifier.OPEN)
                addParameter("compilerArgs", rootCompilerArgsClass)
                addParameter("defaultArgs", rootCompilerArgsClass)
                addStatement("_restrictedArgViolations.clear()")
                if (ownViolationInfos.isNotEmpty()) {
                    for (info in ownViolationInfos) {
                        addViolationCheckStatement(info, "compilerArgs", "defaultArgs", restrictedArgViolationClass)
                    }
                }
            }
        } else {
            val levelCompilerArgsClass = level.getCompilerArgumentsClassName()
            val hasActiveViolations = ownViolationInfos.any { info ->
                (info.errorSince != null && kotlinVersion >= info.errorSince) || kotlinVersion >= info.warningSince
            }
            if (hasActiveViolations) {
                function("collectRestrictedArgViolations") {
                    addModifiers(KModifier.INTERNAL, KModifier.OVERRIDE)
                    addAnnotation(
                        AnnotationSpec.builder(ClassName("kotlin", "Suppress")).addMember("%S", "DEPRECATION").build()
                    )
                    addParameter("compilerArgs", rootCompilerArgsClass)
                    addParameter("defaultArgs", rootCompilerArgsClass)
                    addStatement("super.collectRestrictedArgViolations(compilerArgs, defaultArgs)")
                    addStatement("val args = compilerArgs as %T", levelCompilerArgsClass)
                    addStatement("val castedDefaults = defaultArgs as %T", levelCompilerArgsClass)
                    for (info in ownViolationInfos) {
                        addViolationCheckStatement(info, "args", "castedDefaults", restrictedArgViolationClass)
                    }
                }
            }
        }
    }

    private fun FunSpec.Builder.addViolationCheckStatement(
        info: RestrictedArgInfo,
        argsVarName: String,
        defaultsVarName: String,
        restrictedArgViolationClass: ClassName,
    ) {
        val namesStr = listOfNotNull(info.primaryCli, info.shortName, info.deprecatedName)
            .joinToString("/") { "'$it'" }
        val baseMessage = buildString {
            append("Argument $namesStr is not supported in the Build Tools API.")
            if (info.reason != null) append(" ${info.reason}")
        }
        val warningMessage = buildString {
            append(baseMessage)
            if (info.errorSince != null) {
                append(" This warning will become an error starting from Kotlin ${info.errorSince.releaseName}.")
            }
        }
        val [violationType, message] = when {
            info.errorSince != null && kotlinVersion >= info.errorSince -> "Error" to baseMessage
            kotlinVersion >= info.warningSince -> "Warning" to warningMessage
            else -> return
        }
        addCode(
            CodeBlock.of(
                "if (%L.%N != %L.%N) _restrictedArgViolations.add(%T.%L(%S))\n",
                argsVarName, info.fieldName, defaultsVarName, info.fieldName,
                restrictedArgViolationClass, violationType, message,
            )
        )
    }

    private fun CodeBlock.Builder.maybeAddDelimiterValidation(argument: BtaCompilerArgument<*>) {
        if (argument.delimiter == null) {
            return
        }

        add(
            maybeGetNullabilitySign(argument) + ".also { list -> list.%M(",
            MemberName(targetPackage, "checkNoneContains", isExtension = true)
        )
        add(argument.delimiter)
        add(") }")
    }

    private fun TypeSpec.Builder.maybeAddToArgumentsStringFun(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?) {
        if (!level.isLeaf()) {
            return
        }
        function("toArgumentStrings") {
            addModifiers(KModifier.OVERRIDE)
            if (parentClass == null) {
                addModifiers(KModifier.OPEN)
            }
            returns(listTypeNameOf<String>())
            if (generateCompatLayer) {
                addStatement("val arguments = toCompilerArguments().compilerToArgumentStrings()")
            } else {
                addStatement("val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)")
            }
            addStatement("return arguments")
        }
    }
}

private fun TypeName.isGeneratedEnumList(): Boolean {
    @OptIn(ExperimentalContracts::class)
    contract {
        returns(true) implies (this@isGeneratedEnumList is ParameterizedTypeName)
    }
    return this is ParameterizedTypeName && this.rawType == List::class.asTypeName() && this.typeArguments[0].isGeneratedEnum
}

internal fun generateSafeSetStatement(
    wasIntroducedRecently: Boolean,
    wasRemoved: Boolean,
    name: String,
    argument: BtaCompilerArgument<*>,
    setStatement: CodeBlock,
    generateCompatLayer: Boolean,
): CodeBlock {
    // There's no need in future compatibility check for non-compat layer.
    // The main impl is tied to a compiler version and could not know about the future changes.
    return if (wasRemoved || generateCompatLayer && wasIntroducedRecently) {
        val errorMessage = CodeBlock.of(
            "%P",
            buildString {
                append($$"Compiler parameter not recognized: $$name. Current compiler version is: $KC_VERSION, but")
                if (wasIntroducedRecently) {
                    append(" the argument was introduced in ${argument.introducedSinceVersion.releaseName}")
                }
                if (wasRemoved) {
                    append(if (wasIntroducedRecently) " and" else " the argument was")
                    append(" removed in ${argument.removedSinceVersion?.releaseName}")
                }
            }
        )
        generateSafeMethodAccessStatement(
            setStatement,
            catches = listOf(catchNoSuchMethodError(errorMessage)),
        )
    } else {
        setStatement
    }
}

private fun maybeGetNullabilitySign(argument: BtaCompilerArgument<*>): String = (if (argument.valueType.isNullable) "?" else "")

private fun toCompilerArgumentsAffectingOutcomeFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("toCompilerArgumentsAffectingOutcome").apply {
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter(
        ParameterSpec.builder("arguments", compilerArgumentsClass).apply {
            if (level.isLeaf()) {
                defaultValue("%T()", compilerArgumentsClass)
            }
        }.build()
    )
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
    if (parentClass != null) {
        addStatement("super.toCompilerArgumentsAffectingOutcome(arguments)")
    }
    returns(compilerArgumentsClass)
}

private fun toCompilerArgumentsFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("toCompilerArguments").apply {
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    if (!level.isLeaf()) {
        addParameter("arguments", compilerArgumentsClass)
    } else {
        addStatement(
            "val arguments = %L",
            generateCopyArgumentsBlock(CodeBlock.of("compilerArguments"), CodeBlock.of("%T()", compilerArgumentsClass), level),
        )
    }
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
    if (parentClass != null) {
        addStatement("super.toCompilerArguments(arguments)")
    }
    if (level.isLeaf()) {
        addStatement("val unknownArgs = optionsMap.keys.filterNot { isArgumentKnown(it) }")
        addCode(
            CodeBlock.builder()
                .beginControlFlow("if (unknownArgs.isNotEmpty())")
                .addStatement(
                    "throw %T(\"Unknown arguments: \${unknownArgs.joinToString()}\")",
                    IllegalStateException::class
                )
                .endControlFlow()
                .build()
        )
    }
    returns(compilerArgumentsClass)
}

private fun TypeSpec.Builder.maybeAddApplyArgumentStringsFun(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
    generateCompatLayer: Boolean,
) {
    if (!level.isLeaf()) {
        return
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()

    function("applyArgumentStrings") {
        addModifiers(KModifier.OVERRIDE)
        if (parentClass == null) {
            addModifiers(KModifier.OPEN)
        }
        addParameter("arguments", listTypeNameOf<String>())
        addStatement(
            "val compilerArgs: %T = %M(arguments)",
            compilerArgumentsClass,
            MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments")
        )
        if (!generateCompatLayer) {
            addStatement("collectRestrictedArgViolations(compilerArgs, %T())", compilerArgumentsClass)
            addStatement(
                "%M(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }",
                MemberName("org.jetbrains.kotlin.cli.common.arguments", "validateArgumentsAllErrors"),
            )
            // has to run before the values are applied, so that values previously set through the typed argument API
            // are still observable
            addStatement("argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }")
        } else {
            addStatement(
                "%M(compilerArgs.errors)?.let { throw %M(it) }",
                MemberName("org.jetbrains.kotlin.cli.common.arguments", "validateArguments"),
                MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
            )
        }
        addStatement("applyCompilerArguments(compilerArgs)")
    }
}

private fun applyCompilerArgumentsFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("applyCompilerArguments").apply {

    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter("arguments", compilerArgumentsClass)
    addModifiers(KModifier.PROTECTED)

    if (level.isLeaf()) {
        addStatement(
            "%L",
            generateCopyArgumentsBlock(CodeBlock.of("arguments"), CodeBlock.of("this.compilerArguments"), level)
        )
    }
    if (parentClass != null) {
        addStatement("super.applyCompilerArguments(arguments)")
    }
}

private fun KotlinCompilerArgumentsLevel.getCompilerArgumentsClassName(): ClassName {
    val compilerArgumentInfo = levelToClassNameMap.getValue(name)
    val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
    return compilerArgumentsClass
}

private fun generateSafeMethodAccessStatement(
    codeBlock: CodeBlock,
    catches: List<CodeBlock>,
): CodeBlock {
    val format = buildString {
        append("try { %L }")
        repeat(catches.size) { append(" %L") }
    }
    return CodeBlock.of(format, codeBlock, *catches.toTypedArray())
}

private fun catchCompilerArgumentsParseException(): CodeBlock = CodeBlock.of(
    "catch (ex: %M) { _argumentValidationErrors.add(ex.message ?: %S) }",
    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
    "Error parsing compiler arguments",
)

private fun catchNoSuchMethodError(errorMessage: CodeBlock? = null): CodeBlock =
    if (errorMessage == null) {
        CodeBlock.of("catch (_: NoSuchMethodError) {  }")
    } else {
        CodeBlock.of("catch (e: NoSuchMethodError) { throw IllegalStateException(%L).initCause(e) }", errorMessage)
    }


private fun generateCopyArgumentsBlock(from: CodeBlock, to: CodeBlock, level: KotlinCompilerArgumentsLevel): CodeBlock = CodeBlock.of(
    "%M(%L, %L).also { newArgs -> newArgs.errors = %L.errors } ",
    MemberName(
        "org.jetbrains.kotlin.cli.common.arguments",
        "copy" + level.getCompilerArgumentsClassName().simpleName
    ),
    from, to, from
)
