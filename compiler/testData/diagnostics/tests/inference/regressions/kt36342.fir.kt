// RUN_PIPELINE_TILL: FRONTEND
import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)!!
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!!!
    try {
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } catch (e: Exception) {
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }

    if (true)
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    else
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)

    when {
        true -> <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>) ?: <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, elvisExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, tryExpression, typeParameter, whenExpression */
