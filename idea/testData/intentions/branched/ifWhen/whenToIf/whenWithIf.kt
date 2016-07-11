// KT-12942: when to if changes semantics
fun test(b: Boolean): String {
    <caret>when (b) {
        true ->
            if (true) return "first"
        false ->
            if (true) return "second"
    }

    return "none"
}