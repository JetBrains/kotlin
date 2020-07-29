// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
abstract class A1 {
    abstract val v: Int
    abstract var c1: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

object C1 : A1(){
    override val v: Int
        get() = TODO()
    override var c1: List<Int>
        get() = TODO()
        set(value) {}

    override fun foo() {
        TODO()
    }

    override fun <T> foo1(): T {
        TODO()
    }
}
