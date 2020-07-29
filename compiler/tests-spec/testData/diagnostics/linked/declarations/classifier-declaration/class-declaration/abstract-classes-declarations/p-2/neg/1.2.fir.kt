// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
abstract class A1 {
    abstract val v: Int
    abstract var C: List<Int>
    abstract var c2: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

object C1 : A1(){}

// TESTCASE NUMBER: 2
abstract class A2 {
    abstract val v: Int
    abstract var C: List<Int>
    abstract var c2: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

object C2<!CONSTRUCTOR_IN_OBJECT!>()<!> : A2(){
    override fun foo() {
        TODO()
    }

    override fun <T> foo1(): T {
        TODO()
    }
}

// TESTCASE NUMBER: 3
abstract class A3 {
    abstract val v: Int
    abstract var c: Int
    abstract var c2: List<Int>
    abstract fun foo()
    abstract fun <T> foo3(): T
}

object C3 : A3() {
    override val v: Int
        get() = TODO()
    override var c: Int
        get() = TODO()
        set(value) {}
    override var c2: List<Int>
        get() = TODO()
        set(value) {}
}
