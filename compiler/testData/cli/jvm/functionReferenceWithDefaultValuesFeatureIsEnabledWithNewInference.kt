fun foo(a: String, b: Int = 5): String {
    return a + b
}

fun bar1(body: (String) -> String): String {
    return body("something")
}

fun test() {
    bar1(::foo)
}