// WITH_REFLECT
// FULL_JDK
// TARGET_BACKEND: JVM
// SAM_CONVERSIONS: CLASS
//  ^ SAM-convertion classes created with LambdaMetafactory have no generic signatures

// FILE: Provider.java

public class Provider {
    public <T> String samCall(java.util.concurrent.Callable<? extends T> value) {
        return "fail";
    }
}

// FILE: test.kt

import java.util.concurrent.Callable
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.test.assertEquals

private fun getFirstArgumentType(types: Array<Type>, klass: KClass<*>): String {
    return types
        .filterIsInstance<ParameterizedType>()
        .firstOrNull { it.rawType == klass.java }
        ?.let { it.actualTypeArguments[0] }
        ?.toString() ?: "none"
}

class KtProvider : Provider() {
    override fun <T : Any> samCall(value: Callable<out T>): String =
        getFirstArgumentType(value.javaClass.genericInterfaces, Callable::class)
}

fun interface KtCallable<T> {
    fun invoke(): T
}

fun <T : Any> samCallViaFunInterface(value: KtCallable<out T>): String {
    return getFirstArgumentType(value.javaClass.genericInterfaces, KtCallable::class)
}

fun testCallViaJava(p: Provider): String {
    var result = "not changed"
    wrapInline {
        wrapNoInline {
            result = p.samCall { "str" }
        }
    }
    return result
}

fun testCallViaKotlin(p: KtProvider): String {
    var result = "not changed"
    wrapInline {
        wrapNoInline {
            result = p.samCall { "str" }
        }
    }
    return result
}

fun testCallViaFunInterface(): String {
    var result = "not changed"
    wrapInline {
        wrapNoInline {
            result = samCallViaFunInterface { "str" }
        }
    }
    return result
}


inline fun wrapInline(f: () -> Unit) {
    f()
}

fun wrapNoInline(f: () -> Unit) {
    f()
}

fun box(): String {
    val inferredTypeInSamLambda1 = testCallViaJava(KtProvider())
    val inferredTypeInSamLambda2 = testCallViaKotlin(KtProvider())
    val inferredTypeInSamLambda3 = testCallViaFunInterface()

    assertEquals(inferredTypeInSamLambda1, inferredTypeInSamLambda2)
    assertEquals(inferredTypeInSamLambda2, inferredTypeInSamLambda3)

    return if (inferredTypeInSamLambda1 == "none") "OK" else "fail: $inferredTypeInSamLambda1"
}
