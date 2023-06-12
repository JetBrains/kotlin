// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
<!REDUNDANT_MODIFIER!>open<!> abstract class Base {

    abstract val a: Any
    abstract var b: Any
    internal abstract val c: Any
    internal abstract var d: Any


    abstract fun foo()
    internal abstract fun boo(): Any
}

fun case1() {
    val impl = BaseImplCase1(1, "1", 1.0)
}

class BaseImplCase1(
    override var a: Any, override
    <!VAR_OVERRIDDEN_BY_VAL!>val<!>  b: Any, override var c: Any, override

    <!VAR_OVERRIDDEN_BY_VAL!>val<!>  d: Any = "5") : Base()
{
    override fun foo() {}
    override internal fun boo() {}
}

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2
<!REDUNDANT_MODIFIER!>open<!> abstract class Base {

    abstract val a: Any
    abstract var b: Any
    internal abstract val c: Any
    internal abstract var d: Any


    abstract fun foo()
    internal abstract fun boo(): Any
}

fun case2() {
    val impl = ImplBaseCase2()
}

class ImplBaseCase2() : Base() {
    override var a: Any
        get() = TODO()
        set(value) {}
    override

     <!VAR_OVERRIDDEN_BY_VAL!>val<!>  b: Any
        get() = TODO()
    override var c: Any
        get() = TODO()
        set(value) {}
    override

     <!VAR_OVERRIDDEN_BY_VAL!>val<!>  d: Any
        get() = TODO()

    override fun foo() {}

    override fun boo(): Any {
        return ""
    }
}

// FILE: TestCase3.kt
// TESTCASE NUMBER: 3

package testPackCase3
<!REDUNDANT_MODIFIER!>open<!> abstract class Base {

    abstract val a: Any
    abstract var b: Any
    internal abstract val c: Any
    internal abstract var d: Any


    abstract fun foo()
    internal abstract fun boo(): Any
}

fun case3() {
    ImplBaseCase3()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class ImplBaseCase3<!>() : Base() {
    override var b: Any
        get() = TODO()
        set(value) {}
    override val c: Any
        get() = TODO()
    override var d: Any
        get() = TODO()
        set(value) {}

    override fun foo() {
        TODO()
    }

    override fun boo(): Any {
        TODO()
    }
}

// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackCase4
<!REDUNDANT_MODIFIER!>open<!> abstract class Base {

    abstract val a: Any
    abstract var b: Any
    internal abstract val c: Any
    internal abstract var d: Any


    abstract fun foo()
    internal abstract fun boo(): Any
}

fun case4() {
    ImplBaseCase4()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class ImplBaseCase4<!>() : Base() {
    override var b: Any
        get() = TODO()
        set(value) {}
    override val c: Any
        get() = TODO()
    override var d: Any
        get() = TODO()
        set(value) {}

    override fun foo() {}

    override fun boo(): Any {
        return 1
    }
}

/*
* TESTCASE NUMBER: 5
* NOTE: incompatible modifiers final and abstract
*/
<!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> class Case5() {}
