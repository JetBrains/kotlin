// PARAM_TYPES: kotlin.String?, kotlin.Comparable<String>?, kotlin.CharSequence?, kotlin.Any?
// PARAM_TYPES: X<kotlin.Any?>
// PARAM_DESCRIPTOR: value-parameter val s: kotlin.String? defined in foo
// PARAM_DESCRIPTOR: value-parameter val x: X<kotlin.Any?> defined in foo
class X<T> {
    fun add(t: T) {

    }
}

// SIBLING:
fun foo(s: String?, x: X<Any?>) {
    when {
        s != null -> <selection>x.add(s)</selection>
    }
}