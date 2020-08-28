// !LANGUAGE: +NewInference +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

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

class Case1 : AbstractClassCase1(){
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
    a.<!HIDDEN!>priv<!>()
    a.prot()
    a.int()
    a.pub()

    a.<!HIDDEN!>priv1<!>
    a.prot1
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
    a.<!HIDDEN!>priv<!>()
    a.prot()
    a.int()
    a.pub()

    a.<!HIDDEN!>priv1<!>
    a.prot1
    a.int1
    a.pub1
}
