// FIR_IDENTICAL
class Foo {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.LowPriorityInOverloadResolution
    val test: Bar = Bar()
}

fun Foo.test() {}
class Bar
class Scope {
    operator fun Bar.invoke(f: () -> Unit) {}
}

fun Scope.bar(e: Foo) {
    e.test {}
}
