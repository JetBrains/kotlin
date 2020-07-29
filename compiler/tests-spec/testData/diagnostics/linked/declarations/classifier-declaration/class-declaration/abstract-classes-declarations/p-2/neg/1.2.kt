// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes-declarations -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: abstract members should be implemented in a subtype of the abstract class
 */

// TESTCASE NUMBER: 1
abstract class A1 {
    abstract val v: Int
    abstract var C: List<Int>
    abstract var c2: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>object C1<!> : A1(){}

// TESTCASE NUMBER: 2
abstract class A2 {
    abstract val v: Int
    abstract var C: List<Int>
    abstract var c2: List<Int>
    abstract fun foo()
    abstract fun <T>foo1() : T
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>object C2<!> : A2(){
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

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>object C3<!> : A3() {
    override val v: Int
        get() = TODO()
    override var c: Int
        get() = TODO()
        set(value) {}
    override var c2: List<Int>
        get() = TODO()
        set(value) {}
}