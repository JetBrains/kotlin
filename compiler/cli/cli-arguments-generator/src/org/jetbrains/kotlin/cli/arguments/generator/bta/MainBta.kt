import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.ExplicitApiMode
import org.jetbrains.kotlin.arguments.dsl.types.HasStringValue
import org.jetbrains.kotlin.arguments.dsl.types.JvmTarget
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersion
import org.jetbrains.kotlin.arguments.dsl.types.ReturnValueCheckerMode
import org.jetbrains.kotlin.arguments.dsl.types.StringPathType
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.generators.kotlinpoet.toNullable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.enums.EnumEntries
import kotlin.reflect.KClass

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    generateEnums()
    val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
    while (levels.isNotEmpty()) {
        val level = levels.popLast()
        val parentClass = generateArgumentsForLevel(level.first, level.second)
        levels += level.first.nestedLevels.map { it to parentClass }
    }
}

private const val BTA_PACKAGE = "org.jetbrains.kotlin.buildtools.api.v2"
private const val OUT_PATH = "compiler/build-tools/kotlin-build-tools-api/gen/main/kotlin"

fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName? = null): TypeName {
    val className = level.name.capitalizeAsciiOnly()
    val levelFile = Paths.get(OUT_PATH)
    FileSpec.builder(BTA_PACKAGE, className).apply {
        addType(
            TypeSpec.classBuilder(className).apply {
                parentClass?.let { superclass(it) }
                if (level.nestedLevels.isNotEmpty()) {
                    addModifiers(KModifier.OPEN)
                }
                val argument = generateArgumentType(className)
                val argumentTypeName = ClassName(BTA_PACKAGE, className, argument)
                generateGetPutFunctions(argumentTypeName)
                addType(TypeSpec.companionObjectBuilder().apply {
                    generateOptions(level.arguments, argumentTypeName)
                }.build())
            }.build()
        )
    }.build().writeTo(levelFile)
    return ClassName(BTA_PACKAGE, className)
}

private fun TypeSpec.Builder.generateOptions(arguments: Set<KotlinCompilerArgument>, argumentTypeName: ClassName) {
    arguments.forEach { argument ->

        val name = argument.name.uppercase().replace("-", "_")
        val experimental: Boolean =
            name.startsWith("X") && name != "X"

        val argumentTypeParameter = argument.valueType::class
            .supertypes.single { it.classifier == KotlinArgumentValueType::class }
            .arguments.first().type!!.let {
                when (it.classifier) {
                    in enums -> {
                        ClassName("$BTA_PACKAGE.enums", (it.classifier as KClass<*>).simpleName!!)
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
                    addMember("message = %S", "This option is experimental and it may be changed in the future")
                }
            }
            initializer("%T(%S)", argumentTypeName, name)
        }
    }
}

val enums = listOf(ExplicitApiMode::class, JvmTarget::class, KotlinVersion::class, ReturnValueCheckerMode::class)
val enumValues: List<EnumEntries<*>> =
    listOf(ExplicitApiMode.entries, JvmTarget.entries, KotlinVersion.entries, ReturnValueCheckerMode.entries)

fun generateEnums() {
    enumValues.forEach {
        generateEnum(it)
    }
}

fun generateEnum(sourceEnum: EnumEntries<*>) {
    val className = ClassName("$BTA_PACKAGE.enums", sourceEnum.first()::class.simpleName!!)
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
    }.build().writeTo(Paths.get(OUT_PATH))
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
//    private val optionsMap: MutableMap<CommonCompilerArgument<*>, Any?> = mutableMapOf()
//
//  public operator fun <V> `get`(key: CommonCompilerArgument<V>): V? {
//    @Suppress("UNCHECKED_CAST")
//    return optionsMap[key] as V?
//  }
//
//  public operator fun <V> `set`(key: CommonCompilerArgument<V>, `value`: V?) {
//      optionsMap[key] = value
//  }

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
        returns(typeParameter.toNullable())
        addModifiers(KModifier.OPERATOR)
        addTypeVariable(typeParameter)
        addParameter("key", parameter.parameterizedBy(typeParameter))
        addStatement("return %N[%N] as %T?", mapProperty, "key", typeParameter)
    }
    function("set") {
        val typeParameter = TypeVariableName("V")
        addModifiers(KModifier.OPERATOR)
        addTypeVariable(typeParameter)
        addParameter("key", parameter.parameterizedBy(typeParameter))
        addParameter("value", typeParameter)
        addStatement("%N[%N] = %N", mapProperty, "key", "value")

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
