// RUN_PIPELINE_TILL: FRONTEND
import <!UNRESOLVED_IMPORT!>com<!>.unknown

fun ff() {
    val a = <!UNRESOLVED_REFERENCE!>unknown<!>()
    val b = a?.plus(42)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
