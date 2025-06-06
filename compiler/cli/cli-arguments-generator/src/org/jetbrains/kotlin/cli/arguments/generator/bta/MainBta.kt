/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.arguments.generator.bta

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.enums.EnumEntries
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

fun main(args: Array<String>) {
    val genDir = Paths.get(args[0])
    val apiOnly = args[1] == "api"
    val generator = if (apiOnly) {
        BtaApiGenerator(genDir)
    } else {
        BtaImplGenerator(genDir)
    }
    generator.generateEnums()
    val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
    while (levels.isNotEmpty()) {
        val level = levels.popLast()
        val parentClass = generator.generateArgumentsForLevel(level.first, level.second)
        levels += level.first.nestedLevels.map { it to parentClass }
    }
}

interface BtaGenerator {
    fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName? = null): TypeName
    fun generateEnums()
}

private const val IMPL_PACKAGE = "org.jetbrains.kotlin.buildtools.internal.v2"
private const val API_PACKAGE = "org.jetbrains.kotlin.buildtools.api.v2"

class BtaImplGenerator(val genDir: Path) : BtaGenerator {

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?): TypeName {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val className = apiClassName + "Impl"
        FileSpec.builder(IMPL_PACKAGE, className).apply {
            addType(
                TypeSpec.classBuilder(className).apply {
                    parentClass?.let { superclass(it) }
                    addSuperinterface(ClassName(API_PACKAGE, level.name.capitalizeAsciiOnly()))
                    if (level.nestedLevels.isNotEmpty()) {
                        addModifiers(KModifier.OPEN)
                    }
                    val converterFun = FunSpec.builder("toCompilerArguments").apply {
                        annotation<Suppress> {
                            addMember("%S", "DEPRECATION")
                        }
                        val compilerArgumentInfo = levelToClassNameMap.getValue(level.name)
                        val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
                        addParameter(
                            ParameterSpec.builder("arguments", compilerArgumentsClass).apply {
                                if (level.nestedLevels.isEmpty()) {
                                    defaultValue("%T()", compilerArgumentsClass)
                                }
                            }.build()
                        )
                        returns(compilerArgumentsClass)
                    }

                    val argument = generateArgumentType(apiClassName)
                    val argumentTypeName = ClassName(API_PACKAGE, apiClassName, argument)
                    generateGetPutFunctions(argumentTypeName)
                    generateOptions(level.arguments, apiClassName, converterFun)
                    converterFun.addStatement("return arguments")
                    addFunction(converterFun.build())
                }.build()
            )
        }.build().writeTo(genDir)
        return ClassName(IMPL_PACKAGE, className)
    }

    private fun generateOptions(
        arguments: Set<KotlinCompilerArgument>,
        apiClassName: String,
        converterFun: FunSpec.Builder,
    ) {
        arguments.forEach { argument ->
            val name = argument.name.uppercase().replace("-", "_")
            val clazz = argument.valueType::class
                .supertypes.single { it.classifier == KotlinArgumentValueType::class }
                .arguments.first().type!!.classifier as KClass<*>

            val member = MemberName(ClassName(API_PACKAGE, apiClassName, "Companion"), name)
            when {
                HasStringValue::class in clazz.allSuperclasses -> converterFun.addStatement(
                    "if (%M in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.value }",
                    member,
                    calculateName(argument),
                    member
                )
                argument.valueType is StringPathType || argument.valueType is IntType -> converterFun.addStatement(
                    "if (%M in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.toString() }",
                    member,
                    calculateName(argument),
                    member
                )
                else -> converterFun.addStatement("if (%M in optionsMap) { arguments.%N = get(%M) }", member, calculateName(argument), member)
            }

        }
    }

    override fun generateEnums() {
    }

    fun generateArgumentType(argumentsClassName: String): String {
        require(argumentsClassName.endsWith("Arguments"))
        val argumentTypeName = argumentsClassName.removeSuffix("s")
        return argumentTypeName
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
//  private val optionsMap: MutableMap<CommonCompilerArgument<*>, Any?> = mutableMapOf()
//
//  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V? {
//    @Suppress("UNCHECKED_CAST")
//    return optionsMap[key] as V?
//  }
//
//  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V?) {
//      optionsMap[key] = value
//  }

        // TODO public val arguments: Set<JvmCompilerArgument<*>> get() = optionsMap.keys

        val mapProperty = property(
            "optionsMap",
            ClassName("kotlin.collections", "MutableMap").parameterizedBy(parameter.parameterizedBy(STAR), ANY.copy(nullable = true))
        ) {
            addModifiers(KModifier.PRIVATE)
            initializer("%M()", MemberName("kotlin.collections", "mutableMapOf"))
        }
        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement("return %N[%N] as %T", mapProperty, "key", typeParameter)
        }
        function("set") {
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[%N] = %N", mapProperty, "key", "value")

        }
    }

}

class BtaApiGenerator(val genDir: Path) : BtaGenerator {

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?): TypeName {
        val className = level.name.capitalizeAsciiOnly()
        FileSpec.builder(API_PACKAGE, className).apply {
            addType(
                TypeSpec.interfaceBuilder(className).apply {
                    parentClass?.let { addSuperinterface(it) }
                    val argument = generateArgumentType(className)
                    val argumentTypeName = ClassName(API_PACKAGE, className, argument)
                    generateGetPutFunctions(argumentTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(level.arguments, argumentTypeName)
                    }.build())

                }.build()
            )
        }.build().writeTo(genDir)
        return ClassName(API_PACKAGE, className)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Set<KotlinCompilerArgument>,
        argumentTypeName: ClassName,
    ) {
        arguments.forEach { argument ->

            val name = argument.name.uppercase().replace("-", "_")
            val experimental: Boolean =
                name.startsWith("X") && name != "X"

            val argumentTypeParameter = argument.valueType::class
                .supertypes.single { it.classifier == KotlinArgumentValueType::class }
                .arguments.first().type!!.let {
                    when (it.classifier) {
                        in enums -> {
                            ClassName("$API_PACKAGE.enums", (it.classifier as KClass<*>).simpleName!!)
                        }
                        String::class if argument.valueType is StringPathType -> {
                            Path::class.asTypeName()
                        }
                        else -> {
                            it.asTypeName()
                        }
                    }
                }
                .copy(nullable = argument.valueType.isNullable.current)
            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                annotation<JvmField>()
                addKdoc(argument.description.current)
                if (experimental) {
                    // TODO only as placeholder, should be experimental opt in or something like that, or not generated at all
                    annotation<Deprecated>() {
                        addMember("message = %S", "This option is experimental and it may` be changed in the future")
                    }
                }
                initializer("%T(%S)", argumentTypeName, name)
            }
        }
    }

    val enums = listOf(ExplicitApiMode::class, JvmTarget::class, KotlinVersion::class, ReturnValueCheckerMode::class)
    val enumValues: List<EnumEntries<*>> =
        listOf(ExplicitApiMode.entries, JvmTarget.entries, KotlinVersion.entries, ReturnValueCheckerMode.entries)

    override fun generateEnums() {
        enumValues.forEach {
            generateEnum(it)
        }
    }

    fun generateEnum(sourceEnum: EnumEntries<*>) {
        val className = ClassName("$API_PACKAGE.enums", sourceEnum.first()::class.simpleName!!)
        FileSpec.builder(className).apply {
            addType(TypeSpec.enumBuilder(className).apply {
                property<String>("value") {
                    initializer("value")
                }
                primaryConstructor(FunSpec.constructorBuilder().addParameter("value", String::class).build())
                sourceEnum.forEach {
                    addEnumConstant(
                        it.name,
                        TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", (it as HasStringValue).stringValue)
                            .build()
                    )
                }
            }.build())
        }.build().writeTo(genDir)
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

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
//  private val optionsMap: MutableMap<CommonCompilerArgument<*>, Any?> = mutableMapOf()
//
//  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V? {
//    @Suppress("UNCHECKED_CAST")
//    return optionsMap[key] as V?
//  }
//
//  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V?) {
//      optionsMap[key] = value
//  }

        // TODO public val arguments: Set<JvmCompilerArgument<*>> get() = optionsMap.keys

        function("get") {
            addModifiers(KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
        }
        function("set") {
            addModifiers(KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)

        }
    }
}



interface BtaConverter<S, T> {
    fun convert(value: S): T
}

class NoopConverter : BtaConverter<String, String> {
    override fun convert(value: String): String = value
}

class PathStringConverter : BtaConverter<Path, String> {
    override fun convert(value: Path): String {
        return value.toString()
    }
}
