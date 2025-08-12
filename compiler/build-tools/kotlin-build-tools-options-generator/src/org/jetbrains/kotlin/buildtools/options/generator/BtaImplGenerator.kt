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
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class BtaImplGenerator() : BtaGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?, skipXX: Boolean): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val implClassName = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.Companion.builder(IMPL_PACKAGE, implClassName).apply {
            addAnnotation(
                AnnotationSpec.Companion.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            addType(
                TypeSpec.Companion.classBuilder(implClassName).apply {
                    addModifiers(KModifier.INTERNAL)
                    parentClass?.let { superclass(it) }
                    addSuperinterface(ClassName(API_PACKAGE, level.name.capitalizeAsciiOnly()))
                    if (level.nestedLevels.isNotEmpty()) {
                        addModifiers(KModifier.OPEN)
                    }
                    val converterFun = FunSpec.Companion.builder("toCompilerArguments").apply {
                        val compilerArgumentInfo = levelToClassNameMap.getValue(level.name)
                        val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
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
                        }
                        returns(compilerArgumentsClass)
                    }

                    val argument = generateArgumentType(apiClassName)
                    val argumentTypeName = ClassName(API_PACKAGE, apiClassName, argument)
                    val argumentImplTypeName = ClassName(IMPL_PACKAGE, implClassName, argument)
                    generateGetPutFunctions(argumentTypeName, argumentImplTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(level.filterOutDroppedArguments(), apiClassName, argumentImplTypeName, converterFun, skipXX)
                    }.build())
                    converterFun.addStatement("return arguments")
                    addFunction(converterFun.build())
                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(IMPL_PACKAGE, implClassName), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<KotlinCompilerArgument>,
        apiClassName: String,
        argumentTypeName: ClassName,
        converterFun: FunSpec.Builder,
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
            val member = MemberName(ClassName(API_PACKAGE, apiClassName, "Companion"), name)
            when {
                type.classifier in enumNameAccessors -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.stringValue }",
                    name,
                    argument.calculateName(),
                    member
                )
                argument.valueType is IntType -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.toString() }",
                    name,
                    argument.calculateName(),
                    member
                )
                else -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M) }",
                    name,
                    argument.calculateName(),
                    member
                )
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
            annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[key.id] = %N", mapProperty, "value")
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
}