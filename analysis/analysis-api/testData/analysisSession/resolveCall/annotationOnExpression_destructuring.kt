fun foo(data: Any) {
    <expr>@Suppress("UNCHECKED_CAST")</expr>
    val (k, v) = data as Pair<String, String>
}
