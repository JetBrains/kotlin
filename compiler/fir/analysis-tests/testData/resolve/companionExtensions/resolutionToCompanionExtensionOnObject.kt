// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion object {
        fun test() {
            foo()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>baz<!>()
        }
    }
}

object O {
    fun test() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>bar<!>()
        <!UNRESOLVED_REFERENCE!>baz<!>()
    }
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun C.foo() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun C.Companion.bar() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun O.baz() {}

fun test() {
    C.foo()
    C.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>bar<!>()
    C.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>baz<!>()

    C.Companion.<!UNRESOLVED_REFERENCE!>foo<!>()
    C.Companion.<!UNRESOLVED_REFERENCE!>bar<!>()
    C.Companion.<!UNRESOLVED_REFERENCE!>baz<!>()

    O.<!UNRESOLVED_REFERENCE!>foo<!>()
    O.<!UNRESOLVED_REFERENCE!>bar<!>()
    O.<!UNRESOLVED_REFERENCE!>baz<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, lambdaLiteral, objectDeclaration */
