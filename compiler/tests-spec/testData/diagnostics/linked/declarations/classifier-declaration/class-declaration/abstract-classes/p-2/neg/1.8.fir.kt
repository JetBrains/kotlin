// !LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * NUMBER: 8
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * ISSUES: KT-27825
 */



// MODULE: base
// FILE: AbstractClassCase1.kt
package base

// TESTCASE NUMBER: 1
abstract class AbstractClassCase1() {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> fun priv()
    protected abstract fun prot()
    internal abstract fun int()
    public abstract fun pub()

    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> val priv1: String
    protected abstract val prot1: String
    internal abstract val int1: String
    public abstract val pub1: String
}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case1<!> : AbstractClassCase1(){
    override fun prot() {}

    override fun int() {
        prot()
    }

    override fun pub() {}

    override val prot1: String
        get() = ""
    override val int1: String
        get() = ""
    override val pub1: String
        get() = ""

}

fun case1(){
    val a = Case1()
    a.<!INVISIBLE_REFERENCE!>priv<!>()
    a.<!INVISIBLE_REFERENCE!>prot<!>()
    a.int()
    a.pub()

    a.<!INVISIBLE_REFERENCE!>priv1<!>
    a.<!INVISIBLE_REFERENCE!>prot1<!>
    a.int1
    a.pub1
}

//MODULE: implBase(base)
//FILE: Impl.kt
package implBase
import base.*

// TESTCASE NUMBER: 2
fun case2() {
    val a = Case1()
    a.<!INVISIBLE_REFERENCE!>priv<!>()
    a.<!INVISIBLE_REFERENCE!>prot<!>()
    a.<!INVISIBLE_REFERENCE!>int<!>()
    a.pub()

    a.<!INVISIBLE_REFERENCE!>priv1<!>
    a.<!INVISIBLE_REFERENCE!>prot1<!>
    a.<!INVISIBLE_REFERENCE!>int1<!>
    a.pub1
}
