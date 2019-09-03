// Issue: KT-31734

val x: (@Foo() () -> Unit) get() = {}

val x: @Foo() (() -> Unit) get() = {}

val x: @Foo() () -> Unit get() = {}

val x: Comparable<@Fo() @Bar(10) @Foo() () -> Unit> get() = {}

val x: Any = {} as @Foo() () -> Unit

fun foo(x: (@Foo() () -> Unit)) = x

        fun foo(x: @Foo(10) @Bar() () -> Unit = { x: Int -> x }) {}

fun foo() {
    val x: @Foo() @Bar() () -> Unit = {}
}

fun foo() {
    val x = { x: @Foo() () -> () -> Unit -> x }
}

abstract class A {
    abstract var x: @Foo() (() -> (() -> Unit)) -> Int
}

fun foo(vararg x: @Foo() @Bar(10) @Foo() () -> Unit) = 10

fun foo(): @Foo.Bar() () -> Unit = {}

fun foo(): () -> @Foo.Bar() () -> Unit = {}

val x: Any get() = fun(): @Foo() () -> Unit {}

fun foo() {
    var x: (@Foo() ()->()->Unit) -> Unit = {}
}

fun foo(x: Any) {
    if (x as @Foo() @Bar(10) @Foo() () -> Unit is @Foo() @Bar(10) @Foo() () -> Unit) {}
}

fun foo(y: Any) {
    var x = y as (@Foo() () -> () -> Unit) -> Unit
}
