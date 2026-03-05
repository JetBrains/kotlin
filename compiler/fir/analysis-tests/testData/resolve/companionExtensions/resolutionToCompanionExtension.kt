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

typealias TA = C

<!WRONG_MODIFIER_TARGET!>companion<!> fun C.foo() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun TA.fooTa() {}

<!WRONG_MODIFIER_TARGET!>companion<!> val C.prop = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
<!WRONG_MODIFIER_TARGET!>companion<!> val TA.propTa = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>

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

    D.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>()
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
