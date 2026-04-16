// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
package stuff

open class C {
    companion {
        fun insideCompanionBlock() {
            foo()
            fooTa()
            prop
            propTa
        }
    }

    companion object {
        fun insideCompanionObject() {
            foo()
            fooTa()
            prop
            propTa
        }
    }
}

class D : C() {
    companion {
        fun insideCompanionBlock() {
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fooTa<!>()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>prop<!>
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>propTa<!>
        }
    }

    companion object {
        fun insideCompanionObject() {
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fooTa<!>()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>prop<!>
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>propTa<!>
        }
    }
}

typealias TA = C

companion fun C.foo() {}
companion fun TA.fooTa() {}

companion val C.prop = 1
companion val TA.propTa = 1

fun C.bar() {}

inline fun <reified T> test() {
    C.foo()
    TA.foo()
    C.fooTa()
    TA.fooTa()
    C.prop
    TA.prop
    C.propTa
    TA.propTa

    D.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("companion fun C.foo(): Unit")!>foo<!>()
    D.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>fooTa<!>()
    <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
    D.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>prop<!>
    D.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>propTa<!>
    <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!UNRESOLVED_REFERENCE!>prop<!>

    C.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
    TA.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
    C().<!UNRESOLVED_REFERENCE!>foo<!>()
    C().<!UNRESOLVED_REFERENCE!>prop<!>

    stuff.<!UNRESOLVED_REFERENCE!>foo<!>()
    stuff.<!UNRESOLVED_REFERENCE!>fooTa<!>()
    stuff.<!UNRESOLVED_REFERENCE!>prop<!>
    stuff.<!UNRESOLVED_REFERENCE!>propTa<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
