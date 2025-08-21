// RUN_PIPELINE_TILL: FRONTEND
import <!UNRESOLVED_REFERENCE!>com<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>unknown<!>

fun ff() {
    val a = <!UNRESOLVED_REFERENCE!>unknown<!>()
    val b = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>?.<!DEBUG_INFO_MISSING_UNRESOLVED!>plus<!>(42)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
