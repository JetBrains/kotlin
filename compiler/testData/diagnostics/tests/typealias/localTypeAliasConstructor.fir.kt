// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Cell<TC>(val x: TC)

fun <T> id(x: T): T {
    <!UNSUPPORTED!>typealias C = Cell<T><!>
    class Local(val cell: <!UNRESOLVED_REFERENCE!>C<!>)
    val cx = <!UNRESOLVED_REFERENCE!>C<!>(x)
    val c: <!UNRESOLVED_REFERENCE!>C<!> = Local(cx).cell
    return c.<!UNRESOLVED_REFERENCE!>x<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, nullableType,
primaryConstructor, propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
