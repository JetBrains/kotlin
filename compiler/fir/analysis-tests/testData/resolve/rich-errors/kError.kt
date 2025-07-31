// RUN_PIPELINE_TILL: FRONTEND

error object E1
error object E2

fun getIntOrError(v: Int | KError) {}
fun getNullOrError(v: Nothing? | KError) {}
fun getNothingOrError(v: Nothing | KError) {}
fun getError(v: KError) {}
fun getSpecificError(v: E1) {}

fun foo() {
    val kError: KError = E1
    val eOr: E1 | E2 = E1

    getIntOrError(4)
    getIntOrError(E1)
    getIntOrError(E2)
    getIntOrError(eOr)
    getIntOrError(kError)
    getIntOrError(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    getIntOrError(null!!)

    getNullOrError(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
    getNullOrError(E1)
    getNullOrError(E2)
    getNullOrError(eOr)
    getNullOrError(kError)
    getNullOrError(null)
    getNullOrError(null!!)

    getNothingOrError(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
    getNothingOrError(E1)
    getNothingOrError(E2)
    getNothingOrError(eOr)
    getNothingOrError(kError)
    getNothingOrError(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    getNothingOrError(null!!)

    getError(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
    getError(E1)
    getError(E2)
    getError(eOr)
    getError(kError)
    getError(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    getError(null!!)

    getSpecificError(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
    getSpecificError(E1)
    getSpecificError(<!ARGUMENT_TYPE_MISMATCH!>E2<!>)
    getSpecificError(<!ARGUMENT_TYPE_MISMATCH!>eOr<!>)
    getSpecificError(<!ARGUMENT_TYPE_MISMATCH!>kError<!>)
    getSpecificError(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    getSpecificError(null!!)
}
