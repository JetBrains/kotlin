// !DIAGNOSTICS: -REDUNDANT_NULLABLE

fun test(d: Any, dl: Collection<dynamic>) {
    d as dynamic
    d as dynamic?

    d as? dynamic
    d as? dynamic?

    d is dynamic
    d is dynamic?

    d !is dynamic
    d !is dynamic?

    when (d) {
        is dynamic -> {}
        is dynamic? -> {}
        !is dynamic -> {}
        !is dynamic? -> {}
    }

    dl as List<dynamic>
    dl is List<dynamic>

    when (dl) {
        is List<dynamic> -> {}
    }
}
