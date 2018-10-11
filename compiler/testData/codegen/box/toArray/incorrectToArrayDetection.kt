// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

import java.lang.reflect.Modifier

abstract class A : Collection<String> {
    protected fun <T> foo(x: Array<T>): Array<T> = x
}

fun box(): String {
    val method = A::class.java.declaredMethods.single { it.name == "foo" }
    return if (Modifier.isProtected(method.modifiers)) "OK" else "Fail: $method"
}
