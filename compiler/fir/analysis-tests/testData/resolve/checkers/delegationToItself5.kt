// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77451, KT-17417

object B : A by B {
    override val bar = ""
    override fun foo() = 2
}

interface A {
    val bar get() = "str"
    fun foo() = 1
}
