// !LANGUAGE: -DoNotGenerateThrowsForDelegatedKotlinMembers
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FILE: A.kt

import java.io.IOException

interface A {
    @Throws(IOException::class)
    fun foo()
}

// FILE: B.kt

class B(a: A) : A by a

fun box(): String {
    val method = B::class.java.declaredMethods.single { it.name == B::foo.name }
    if (method.exceptionTypes.size != 1)
        return "Fail: ${method.exceptionTypes.toList()}"

    return "OK"
}
