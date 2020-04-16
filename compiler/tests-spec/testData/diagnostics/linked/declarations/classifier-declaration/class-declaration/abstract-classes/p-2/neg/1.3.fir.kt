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
    override fun foo(): Any
    {
        return ""
    }

    override val a: Any?
    get() = TODO()
    override var b: String
    get() = TODO()
    set(value)
    {}
}


/*
* TESTCASE NUMBER: 2
*/

class Case2(override val a: String, override var b: String) : Base() {
    override fun foo(): CharSequence? {
        return ""
    }
}

/*
* TESTCASE NUMBER: 3
* NOTE: abstract nested class members are not implemented
*/

class Case3 {
    class ImplBase1 : MainClass.Base1() {}
}

class MainClass {
    abstract class Base1() {
        abstract val a: CharSequence
        abstract var b: CharSequence

        abstract fun foo(): CharSequence
    }

}
