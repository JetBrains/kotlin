// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES

// FILE: lib.kt
package test
import java.lang.reflect.*

open class TypeBase<T>

inline fun <reified T> reifiedType(): Type {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!
    return (superType as ParameterizedType).actualTypeArguments.first()!!
}

// FILE: main.kt
package test
import kotlin.coroutines.*
import java.lang.reflect.*
import kotlin.test.assertEquals

open class MyClass {
    open suspend fun <T> fooTypeParameter() = reifiedType<Foo<T>>()
}

class Foo<T>

fun runBlocking(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext){
        it.getOrThrow()
    })
}

fun box(): String {
    runBlocking {
        assertEquals("test.Foo<T>", MyClass().fooTypeParameter<String>().toString())
    }
    return "OK"
}