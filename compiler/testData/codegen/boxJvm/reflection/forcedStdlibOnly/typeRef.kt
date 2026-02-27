// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION
// CHECK_BYTECODE_TEXT

// FILE: J.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class J {
    public static String nullabilityFlexible() { return null; }

    @NotNull
    public static List<String> mutabilityFlexible() { return null; }

    public static List<String> bothFlexible() { return null; }
}

// FILE: A.kt

open class A

// FILE: test.kt

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.jvm.internal.TypeReference
import kotlin.jvm.internal.ClassReference
import kotlin.reflect.typeOf

inline fun <reified T> returnTypeOf(block: () -> T) = typeOf<T>()

fun <T> nonReifiedParamType(x: T) = typeOf<List<T>>().arguments.first().type!!

class G<T : A> {
    fun nonReifiedClassParamType() = typeOf<List<T>>().arguments.first().type!!
}

val lightTypes = listOf(
    // simple
    typeOf<A>(),
    typeOf<Any>(),
    typeOf<List<Nothing>>().arguments[0].type!!,
    typeOf<Unit>(),
    typeOf<List<String>>(),
    typeOf<MutableList<String>>(),
    typeOf<Int>(),
    typeOf<String>(),
    // nullable
    typeOf<A?>(),
    typeOf<Any>(),
    typeOf<List<Nothing?>>().arguments[0].type!!,
    typeOf<Unit?>(),
    typeOf<List<String?>?>(),
    typeOf<MutableList<String?>?>(),
    typeOf<Int?>(),
    typeOf<String?>(),
    // variance
    typeOf<List<out A>>(),
    typeOf<List<*>>(),
    typeOf<MutableList<in A>>(),
    typeOf<MutableList<out A>>(),
    typeOf<MutableList<*>>(),
    // flexible
    returnTypeOf { J.nullabilityFlexible() },
    returnTypeOf { J.mutabilityFlexible() },
    returnTypeOf { J.bothFlexible() },
    // from non-reified type parameter
    nonReifiedParamType(1),
    G<A>().nonReifiedClassParamType()
)

fun isLightType(type: KType): Boolean = type is TypeReference
fun isLightClass(klass: KClass<*>): Boolean = klass is ClassReference

fun box(): String {
    for (type in lightTypes) {
        if (!isLightType(type)) return "Failed for type $type"
        (type.classifier as? KClass<*>)?.let {
            if (!isLightClass(it)) return "Failed for classifier of type $type"
        }
        for (typeArg in type.arguments) {
            typeArg.type?.let {
                if (!isLightType(it)) return "Failed for type argument of $type: $typeArg"
            }
        }
    }

    return "OK"
}

// @TestKt.class:
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection
// 65 INVOKESTATIC kotlin/jvm/internal/StdlibOnlyReflection
