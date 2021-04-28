// !LANGUAGE: +ProperIeee754Comparisons

val minus: Any = -0.0

fun box(): String {
    if (minus is Comparable<*> && minus is Double) {
        if (minus < 0.0) return "fail 0"
        if ((minus) != 0.0) return "fail 1"
        if (minus != 0.0) return "fail 2"
        // TODO: FE1.0 allows comparison of incompatible type after smart cast (KT-46383) but FIR rejects it. We need to figure out a transition plan.
        // if (minus != 0.0F) return "fail 3"
    }
    return "OK"
}