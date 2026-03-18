// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17301
// WITH_STDLIB

// KT-17301: Long chained expressions compilation fails with StackOverflowError

fun test() {
    val v = A(0)
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
            .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

    println(v.a)
}

class A(a_init: Int) {
    val a = a_init
    fun foo() = A(a + 1)
}
