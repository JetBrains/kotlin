fun bar(b: Boolean) = b

fun foo(data: List<String>) {
    bar(data.contains(""))
}
