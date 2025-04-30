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
    WithoutMRV.A // Should we ALWAYS report enum entries?
    WithoutMRV.A.foo()
}
