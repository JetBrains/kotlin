// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

fun <T> id(t: T): T = t

class Generic<T>
fun <D> expectGeneric(d: Generic<D>) = Unit

fun test() {
    val a: String = <!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>
    val b: String = id(<!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>)
    id<String>(<!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>expectGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>expectGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>))
    <!CANNOT_INFER_PARAMETER_TYPE!>expectGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE!>run<!> {
        <!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>
    })
    val c: Generic<String> = id(<!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>)
    val d: Generic<String> = run {
        <!UNRESOLVED_COLLECTION_LITERAL!>["!"]<!>
    }
    val e: Array<String> = [<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>]
    val f: Array<Array<String>> = [[<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>]]
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, propertyDeclaration,
stringLiteral, typeParameter */
