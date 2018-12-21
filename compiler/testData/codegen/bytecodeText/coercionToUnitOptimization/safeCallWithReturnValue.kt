// IGNORE_BACKEND: JVM_IR
interface A {
    fun foo(): Any
}

fun test(x: A?) {
    x?.foo()
}

// 2 POP
// 0 ACONST_NULL