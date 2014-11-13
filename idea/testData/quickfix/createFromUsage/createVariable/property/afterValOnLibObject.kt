// "Create property 'foo'" "true"
// ERROR: Property must be initialized

fun test() {
    val a: Int = Unit.foo
}

val Unit.foo: Int
