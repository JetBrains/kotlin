// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun f(vararg x: Int) {}

val inVal: (vararg x: Int)->Unit = {}

fun inParam(fn: (vararg x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (vararg n: Int)->Unit)->Unit) {}

fun inReturn(): (vararg x: Int)->Unit = {}

class A : (vararg Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (vararg x: Int)->Unit = {}
    }

    val prop: (vararg x: Int)->Unit
        get(): (vararg x: Int)->Unit = {}
}

val allProhibited: (<!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>abstract<!>
                    annotation
                    companion
                    <!INCOMPATIBLE_MODIFIERS!>const<!>
                    <!INCOMPATIBLE_MODIFIERS!>crossinline<!>
                    <!INCOMPATIBLE_MODIFIERS!>data<!>
                    enum
                    external
                    <!INCOMPATIBLE_MODIFIERS!>final<!>
                    <!INCOMPATIBLE_MODIFIERS!>in<!>
                    <!INCOMPATIBLE_MODIFIERS!>inline<!>
                    <!INCOMPATIBLE_MODIFIERS!>inner<!>
                    <!INCOMPATIBLE_MODIFIERS!>internal<!>
                    lateinit
                    <!INCOMPATIBLE_MODIFIERS!>noinline<!>
                    <!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>open<!>
                    operator
                    <!INCOMPATIBLE_MODIFIERS!>out<!>
                    <!INCOMPATIBLE_MODIFIERS!>override<!>
                    <!INCOMPATIBLE_MODIFIERS!>private<!>
                    <!INCOMPATIBLE_MODIFIERS!>protected<!>
                    <!INCOMPATIBLE_MODIFIERS!>public<!>
                    reified
                    <!INCOMPATIBLE_MODIFIERS!>sealed<!>
                    tailrec
                    vararg

                    x: Int)->Unit = {}

val valProhibited: (val x: Int)->Unit = {}
val varProhibited: (var x: Int)->Unit = {}