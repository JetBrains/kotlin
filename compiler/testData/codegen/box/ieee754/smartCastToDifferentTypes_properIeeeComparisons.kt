// !LANGUAGE: +ProperIeee754Comparisons

// TODO: FE1.0 allows comparison of incompatible type after smart cast (KT-46383) but FIR rejects it. We need to figure out a transition plan.
//fun ne(x: Any, y: Any) = x is Double && y is Float && x != y
fun lt(x: Any, y: Any) = x is Double && y is Float && x < y
fun gt(x: Any, y: Any) = x is Double && y is Float && x > y

fun box(): String {
    // if (ne(0.0, -0.0F)) return "fail 1"
    if (lt(0.0, -0.0F)) return "fail 2"
    if (gt(0.0, -0.0F)) return "fail 3"

    return "OK"
}
