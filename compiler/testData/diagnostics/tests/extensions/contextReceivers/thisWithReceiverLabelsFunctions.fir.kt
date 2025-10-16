// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<String>) fun A<Int>.f() {
    this@A.a.<!UNRESOLVED_REFERENCE!>length<!>
}

context(A<String>, B) fun f() {
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@B<!>.b
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun f() {
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@B<!>.b
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun C.f() {
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@B<!>.b
    this@C.c
    this@f.c
    this.c
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
nullableType, primaryConstructor, propertyDeclaration, thisExpression, typeParameter */
