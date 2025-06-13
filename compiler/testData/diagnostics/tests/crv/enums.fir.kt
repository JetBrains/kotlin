// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB


@MustUseReturnValue
enum class WithMRV {
    A, B;
    fun foo() = ""
}

enum class WithoutMRV {
    A, B;
    fun foo() = ""
}

fun main() {
    <!RETURN_VALUE_NOT_USED!>WithMRV.A<!>
    <!RETURN_VALUE_NOT_USED!>WithMRV.A.foo()<!>
    <!RETURN_VALUE_NOT_USED!>WithoutMRV.A<!> // Should we ALWAYS report enum entries?
    WithoutMRV.A.foo()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
