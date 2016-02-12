// PARAM_TYPES: kotlin.String
// PARAM_DESCRIPTOR: value-parameter p: kotlin.Any defined in foo

fun foo(p: Any) {
    if (p is String) {
        <selection>f(p)
        g(p)</selection>
    }
}

fun f(s: String){}
fun g(o: Any){}