// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB


@MustUseReturnValues
enum class WithMRV {
    A, B;
    fun foo() = ""
}

enum class WithoutMRV {
    A, B;
    fun foo() = ""
}

fun main() {
    WithMRV.<!RETURN_VALUE_NOT_USED!>A<!>
    WithMRV.A.<!RETURN_VALUE_NOT_USED!>foo<!>()
    WithoutMRV.<!RETURN_VALUE_NOT_USED!>A<!> // Should we ALWAYS report enum entries?
    WithoutMRV.A.foo()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
