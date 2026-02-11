// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<String>, B) var p: Int
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }

context(A<Int>, A<String>, B) var p: Int
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.toDouble()
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }

context(A<Int>, A<String>, B) val C.p: Int
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        this<!UNRESOLVED_LABEL!>@C<!>.c
        this@p.c
        this.c
        return 1
    }

/* GENERATED_FIR_TAGS: assignment, classDeclaration, getter, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, propertyDeclarationWithContext, propertyWithExtensionReceiver, setter, thisExpression,
typeParameter */
