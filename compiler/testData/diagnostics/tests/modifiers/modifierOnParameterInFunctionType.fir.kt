// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun f(vararg x: Int) {}

val inVal: (<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> x: Int)->Unit = {}

fun inParam(fn: (<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> n: Int)->Unit)->Unit) {}

fun inReturn(): (vararg x: Int)->Unit = {}

class A : (vararg Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (vararg x: Int)->Unit = {}
    }

    val prop: (vararg x: Int)->Unit
        get(): (<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> x: Int)->Unit = {}
}

val allProhibited: (<!REDUNDANT_MODIFIER, WRONG_MODIFIER_TARGET!>abstract<!>
                    <!WRONG_MODIFIER_TARGET!>annotation<!>
                    <!WRONG_MODIFIER_TARGET!>companion<!>
                    <!INCOMPATIBLE_MODIFIERS!>const<!>
                    <!INCOMPATIBLE_MODIFIERS!>crossinline<!>
                    <!INCOMPATIBLE_MODIFIERS!>data<!>
                    <!WRONG_MODIFIER_TARGET!>enum<!>
                    <!WRONG_MODIFIER_TARGET!>external<!>
                    <!INCOMPATIBLE_MODIFIERS!>final<!>
                    <!WRONG_MODIFIER_TARGET!>in<!>
                    <!INCOMPATIBLE_MODIFIERS!>inline<!>
                    <!INCOMPATIBLE_MODIFIERS!>inner<!>
                    <!WRONG_MODIFIER_TARGET!>internal<!>
                    <!WRONG_MODIFIER_TARGET!>lateinit<!>
                    <!INCOMPATIBLE_MODIFIERS!>noinline<!>
                    <!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>open<!>
                    <!WRONG_MODIFIER_TARGET!>operator<!>
                    <!INCOMPATIBLE_MODIFIERS!>out<!>
                    <!INCOMPATIBLE_MODIFIERS!>override<!>
                    <!INCOMPATIBLE_MODIFIERS!>private<!>
                    <!INCOMPATIBLE_MODIFIERS!>protected<!>
                    <!INCOMPATIBLE_MODIFIERS!>public<!>
                    <!WRONG_MODIFIER_TARGET!>reified<!>
                    <!INCOMPATIBLE_MODIFIERS!>sealed<!>
                    <!WRONG_MODIFIER_TARGET!>tailrec<!>
                    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!>

                    x: Int)->Unit = {}

val valProhibited: (val x: Int)->Unit = {}
val varProhibited: (var x: Int)->Unit = {}
