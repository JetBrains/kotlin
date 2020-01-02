annotation class Foo {
    annotation class Bar
}

@Foo.Bar
fun box(): String {
    return "OK"
}
