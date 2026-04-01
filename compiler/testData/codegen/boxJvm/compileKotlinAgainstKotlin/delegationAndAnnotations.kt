// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: A.kt

import java.io.IOException

interface A {
    @Throws(IOException::class)
    @Anno
    fun foo()
}

annotation class Anno

// MODULE: main(lib)
// FILE: B.kt

class B(a: A) : A by a

fun box(): String {
    val method = B::class.java.declaredMethods.single { it.name == B::foo.name }
    if (method.exceptionTypes.size != 0)
        return "Fail throws: ${method.exceptionTypes.toList()}"

    if (method.declaredAnnotations.size != 1)
        return "Fail annotations: ${method.declaredAnnotations.toList()}"

    return "OK"
}
