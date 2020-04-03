// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: attempt to implement abstract members with invalid types
 */

// TESTCASE NUMBER: 1
abstract class Base {
    abstract val a: CharSequence
    abstract var b: CharSequence

    abstract fun foo(): CharSequence
}

class Case1 : Base() {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Any<!>
    {
        return ""
    }

    override val a: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Any?<!>
    get() = TODO()
    override var b: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String<!>
    get() = TODO()
    set(value)
    {}
}


/*
* TESTCASE NUMBER: 2
*/

class Case2(override val a: String, override var b: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String<!>) : Base() {
    override fun foo(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>CharSequence?<!> {
        return ""
    }
}

/*
* TESTCASE NUMBER: 3
* NOTE: abstract nested class members are not implemented
*/

class Case3 {
    <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class ImplBase1<!> : MainClass.Base1() {}
}

class MainClass {
    abstract class Base1() {
        abstract val a: CharSequence
        abstract var b: CharSequence

        abstract fun foo(): CharSequence
    }

}


