/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.listTypeNameOf
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class BtaImplGenerator(private val targetPackage: String, private val skipXX: Boolean) : BtaGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val implClassName = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.Companion.builder(targetPackage, implClassName).apply {
            addAliasedImport(MemberName("org.jetbrains.kotlin.compilerRunner", "toArgumentStrings"), "compilerToArgumentStrings")
            addAnnotation(
                AnnotationSpec.Companion.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            addType(
                TypeSpec.Companion.classBuilder(implClassName).apply {
                    addModifiers(KModifier.INTERNAL)
                    if (level.nestedLevels.isNotEmpty()) {
                        addModifiers(KModifier.ABSTRACT)
                    }

                    parentClass?.let { superclass(it) }
                    addSuperinterface(ClassName(API_PACKAGE, level.name.capitalizeAsciiOnly()))

                    property(
                        "internalArguments",
                        ClassName("kotlin.collections", "MutableSet").parameterizedBy(typeNameOf<String>()),
                        KModifier.PRIVATE
                    ) {
                        initializer("%M()", MemberName("kotlin.collections", "mutableSetOf"))
                    }
                    val toCompilerConverterFun = toCompilerConverterFunBuilder(level, parentClass)
                    val applyCompilerArgumentsFun = applyCompilerArgumentsFunBuilder(level, parentClass)
                    val applyArgumentStringsFun = applyArgumentStringsFunBuilder(level, parentClass)
                    val toArgumentStringsFun = if (level.nestedLevels.isEmpty()) {
                        toArgumentsStringFunBuilder(parentClass)
                    } else {
                        null
                    }

                    val argumentTypeNameString = generateArgumentType(apiClassName)
                    val argumentTypeName = ClassName(API_PACKAGE, apiClassName, argumentTypeNameString)
                    val argumentImplTypeName = ClassName(targetPackage, implClassName, argumentTypeNameString)

                    generateGetPutFunctions(argumentTypeName, argumentImplTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(
                            arguments = level.arguments,//.filterOutDroppedArguments(),
                            implClassName = implClassName,
                            argumentTypeName = argumentImplTypeName,
                            applyCompilerArgumentsFun = applyCompilerArgumentsFun,
                            toCompilerConverterFun = toCompilerConverterFun,
                            skipXX = skipXX
                        )
                    }.build())

//                    if (targetPackage == IMPL_PACKAGE) {
                    if (level.nestedLevels.isEmpty()) {
                        toCompilerConverterFun.addStatement(
                            "arguments.internalArguments = %M<%T>(internalArguments.toList()).internalArguments",
                            MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments"),
                            level.getCompilerArgumentsClassName()
                        )
                    }
                    toCompilerConverterFun.addStatement("return arguments")
                    addFunction(toCompilerConverterFun.build())
//                    }
                    addFunction(applyArgumentStringsFun.build())
//                    toArgumentStringsFun.addStatement("arguments.addAll(internalArguments)")
//                    toArgumentStringsFun.addStatement("return arguments")
                    toArgumentStringsFun?.let { addFunction(it.build()) }
                    applyCompilerArgumentsFun.addStatement("internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })")
                    addFunction(applyCompilerArgumentsFun.build())

                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, implClassName), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<KotlinCompilerArgument>,
        implClassName: String,
        argumentTypeName: ClassName,
        applyCompilerArgumentsFun: FunSpec.Builder,
        toCompilerConverterFun: FunSpec.Builder,
        skipXX: Boolean,
    ) {
        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach

            if (argument.releaseVersionsMetadata.removedVersion != null) {
                return@forEach
            }

            // generate impl mirror of arguments
            val type = argument.valueType::class
                .supertypes.single { it.classifier == KotlinArgumentValueType::class }
                .arguments.first().type!!
            val argumentTypeParameter = when (val classifier = type.classifier) {
                is KClass<*> if classifier.isSubclassOf(Enum::class) && classifier in enumNameAccessors -> {
                    ClassName("$API_PACKAGE.enums", classifier.simpleName!!)
                }
                else -> {
                    type.asTypeName()
                }
            }.copy(nullable = argument.valueType.isNullable.current)
            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                initializer("%T(%S)", argumentTypeName, name)
            }

            // add argument to the converter function
            val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)
            when {
                type.classifier in enumNameAccessors -> {
                    toCompilerConverterFun.addSafeMethodAccessStatement(
                        "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.stringValue }",
                        name,
                        argument.calculateName(),
                        member
                    )

//                    toArgumentStringsFun.addStatement(
//                        "if (%S in optionsMap && optionsMap[%S] != null) { arguments.add(%S + get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.stringValue) }",
//                        name,
//                        name,
//                        "-${argument.name}=",
//                        member
//                    )
                    applyCompilerArgumentsFun.addSafeMethodAccessStatement(
                        "this[%M] = arguments.%N${if (argument.valueType.isNullable.current) "?" else ""}.let { %T.entries.first { entry -> entry.stringValue == it } }",
                        member,
                        argument.calculateName(),
                        argumentTypeParameter.copy(nullable = false)
                    )
                }
                argument.valueType is IntType -> {
                    toCompilerConverterFun.addSafeMethodAccessStatement(
                        "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.toString() }",
                        name,
                        argument.calculateName(),
                        member
                    )
//                    toArgumentStringsFun.addStatement(
//                        "if (%S in optionsMap && optionsMap[%S] != null) { arguments.add(%S + get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.toString()) }",
//                        name,
//                        name,
//                        "-${argument.name}=",
//                        member
//                    )
                    applyCompilerArgumentsFun.addSafeMethodAccessStatement(
                        "this[%M] = arguments.%N${if (argument.valueType.isNullable.current) "?" else ""}.let { it.toInt() }",
                        member,
                        argument.calculateName(),
                    )
                }
                else -> {
                    toCompilerConverterFun.addSafeMethodAccessStatement(
                        "if (%S in optionsMap) { arguments.%N = get(%M) }",
                        name,
                        argument.calculateName(),
                        member
                    )
//                    toArgumentStringsFun.addStatement(
//                        "if (%S in optionsMap && optionsMap[%S] != null) { arguments.add(%S + get(%M)) }",
//                        name,
//                        name,
//                        "-${argument.name}=",
//                        member
//                    )
                    applyCompilerArgumentsFun.addSafeMethodAccessStatement(
                        "this[%M] = arguments.%N",
                        member,
                        argument.calculateName(),
                    )
                }
            }
        }
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName, implParameter: ClassName) {
        val mapProperty = property(
            "optionsMap",
            ClassName("kotlin.collections", "MutableMap").parameterizedBy(typeNameOf<String>(), ANY.copy(nullable = true))
        ) {
            addModifiers(KModifier.PRIVATE)
            initializer("%M()", MemberName("kotlin.collections", "mutableMapOf"))
        }
        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            if (targetPackage == IMPL_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED)
            }
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            if (targetPackage == IMPL_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED)
            }
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[key.id] = %N", mapProperty, "value")
        }

        function("contains") {
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            returns(BOOLEAN)
            addParameter("key", parameter.parameterizedBy(STAR))
            addStatement("return key.id in optionsMap")
        }

        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[key.id] = %N", mapProperty, "value")
        }

        function("contains") {
            addModifiers(KModifier.OPERATOR)
            returns(BOOLEAN)
            addParameter("key", implParameter.parameterizedBy(STAR))
            addStatement("return key.id in optionsMap")
        }
    }

    fun TypeSpec.Builder.generateArgumentType(argumentsClassName: String): String {
        require(argumentsClassName.endsWith("Arguments"))
        val argumentTypeName = argumentsClassName.removeSuffix("s")
        val typeSpec =
            TypeSpec.classBuilder(argumentTypeName).apply {
                addTypeVariable(TypeVariableName("V"))
                property<String>("id") {
                    initializer("id")
                }
                primaryConstructor(FunSpec.constructorBuilder().addParameter("id", String::class).build())
            }.build()
        addType(typeSpec)
        return argumentTypeName
    }
}

private fun toArgumentsStringFunBuilder(parentClass: TypeName?): FunSpec.Builder = FunSpec.builder("toArgumentStrings").apply {
    addModifiers(KModifier.OVERRIDE)
//    annotation<Suppress> {
//        addMember("%S", "DEPRECATION")
//    }
//    addAnnotation(
//        AnnotationSpec.Companion.builder(ClassName("kotlin", "OptIn"))
//            .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
//    )
    if (parentClass != null) {
//        addStatement("arguments.addAll(super.toArgumentStrings())")
    } else {
        addModifiers(KModifier.OPEN)
    }
    returns(listTypeNameOf<String>())
    addStatement("val arguments = toCompilerArguments().compilerToArgumentStrings()")
    addStatement("return arguments")
}

private fun toCompilerConverterFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.Companion.builder("toCompilerArguments").apply {
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter(
        ParameterSpec.Companion.builder("arguments", compilerArgumentsClass).apply {
            if (level.nestedLevels.isEmpty()) {
                defaultValue("%T()", compilerArgumentsClass)
            }
        }.build()
    )
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
    if (parentClass != null) {
        addStatement("super.toCompilerArguments(arguments)")
//        addModifiers(KModifier.OVERRIDE)
    } else {
//        addModifiers(KModifier.OPEN)
    }
//    addStatement("return %M(toArgumentStrings())", MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments"))
    returns(compilerArgumentsClass)
}

private fun applyArgumentStringsFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.Companion.builder("applyArgumentStrings").apply {
    addModifiers(KModifier.OVERRIDE)
    if (parentClass == null) {
        addModifiers(KModifier.OPEN)
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter("arguments", listTypeNameOf<String>())
    addStatement(
        "val compilerArgs: %T = %M(arguments)",
        compilerArgumentsClass,
        MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments")
    )
    addStatement("applyCompilerArguments(compilerArgs)")
}

private fun applyCompilerArgumentsFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.Companion.builder("applyCompilerArguments").apply {
    if (parentClass != null) {
        addStatement("super.applyCompilerArguments(arguments)")
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter("arguments", compilerArgumentsClass)
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
}

private fun KotlinCompilerArgumentsLevel.getCompilerArgumentsClassName(): ClassName {
    val compilerArgumentInfo = levelToClassNameMap.getValue(name)
    val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
    return compilerArgumentsClass
}

private fun FunSpec.Builder.addSafeMethodAccessStatement(format: String, vararg args: Any) =
    addStatement("try { $format } catch (_: NoSuchMethodError) {}", *args)