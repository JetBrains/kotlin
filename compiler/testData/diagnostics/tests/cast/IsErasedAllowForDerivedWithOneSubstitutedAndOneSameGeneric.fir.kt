// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
open class BaseTwo<A, B>
open class DerivedWithOne<D>: BaseTwo<D, String>()

// a is BaseTwo<T, U> => if (a is DerivedWithOne<?>) a is DerivedWithOne<T>
fun <T, U> testing(a: BaseTwo<T, U>) = a is <!CANNOT_CHECK_FOR_ERASED_DEPRECATION_WARNING!>DerivedWithOne<T><!>
