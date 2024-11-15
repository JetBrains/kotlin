// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
open class BaseMulti<out A, B>
class SomeMultiDerived<out D>: BaseMulti<D, Any>()

// t is BaseMulti<+String, String> => if (t is SomeMultiDerived<?>) => t is SomeMultiDerived<+String> =>
//     => (String <: Any, SomeMultiDerived<Covariant D>) t is SomeMultiDerived<+Any>
fun someDerived(t: BaseMulti<String, String>) = t is <!CANNOT_CHECK_FOR_ERASED!>SomeMultiDerived<Any><!>
