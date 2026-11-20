// RUN_PIPELINE_TILL: FRONTEND
error class E1

interface I<T>
interface J<T> : I<T> {
    fun foo()
}

fun unionToPrimary(x: I<String> | E1) {
    val _ = x is I
    if (x is J) {
        x.foo()
    }
}

fun simpleToUnion(x: I<String>) {
    if (x is (<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>J<!> | E1)) {
        x<!UNNECESSARY_SAFE_CALL!>|.<!><!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun unionToUnion(x: I<String> | E1) {
    if (x is (<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>J<!> | E1)) {
        x|.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, safeCall, typeParameter */
