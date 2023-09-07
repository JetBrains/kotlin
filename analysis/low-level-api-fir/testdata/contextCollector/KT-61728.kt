fun foo(param: String?): String? {
    val tmp = param?.let {
        <expr>it</expr>
    } ?: return null
}
