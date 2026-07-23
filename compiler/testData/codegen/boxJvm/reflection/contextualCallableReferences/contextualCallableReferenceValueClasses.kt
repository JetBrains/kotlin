// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters +CallableReferencesToContextual

import kotlin.test.assertEquals

@JvmInline
value class Z(val value: String)

// top-level function: value-class value parameter
context(c: String)
fun topLevelValueParam(z: Z): Z = Z(c + z.value)

// top-level extension on a value class: bound extension receiver + bound context argument (two leading bound args)
context(c: String)
fun Z.valueExtension(y: Z): Z = Z(c + value + y.value)

// top-level function returning a value class, no unbound arguments (only the context argument is bound)
context(c: String)
fun onlyContext(): Z = Z(c)

// regular-class member with a value-class value parameter
class A(val a: String) {
    context(c: String)
    fun member(y: Z): Z = Z(a + c + y.value)
}

// top-level function with a value-class CONTEXT parameter
context(z: Z)
fun valueContext(y: Z): Z = Z(z.value + y.value)

fun box(): String {
    context("X") {
        // (1) Static caller with bound args, leading bound args = [X]; one unbound value-class value parameter
        val f1 = ::topLevelValueParam
        assertEquals(Z("Xz"), f1.call(Z("z")))

        // (2) Static caller with bound args, leading bound args = [X, "r"] (context argument + bound value-class extension receiver)
        val f2 = Z("r")::valueExtension
        assertEquals(Z("Xry"), f2.call(Z("y")))

        // (3) Static caller with bound args, leading bound args = [X]; no unbound arguments, value-class return type
        val f3 = ::onlyContext
        assertEquals(Z("X"), f3.call())

        // (4) Instance caller with bound dispatch receiver A("a") + leading bound context argument [X]
        val f4 = A("a")::member
        assertEquals(Z("aXy"), f4.call(Z("y")))

        // (5) Instance caller with leading bound context argument [X]; dispatch receiver passed at call time
        val f5 = A::member
        assertEquals(Z("aXy"), f5.call(A("a"), Z("y")))
    }

    context(Z("q")) {
        // (6) Static caller with leading bound arg = [Z("q")] — a value-class context argument that must be unboxed when prepended
        val f6 = ::valueContext
        assertEquals(Z("qy"), f6.call(Z("y")))
    }
    return "OK"
}
