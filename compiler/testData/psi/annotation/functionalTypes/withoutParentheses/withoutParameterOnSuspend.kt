// Issue: KT-31734

val x: (@Foo suspend () -> Unit) get() = {}

val x: @Foo (suspend () -> Unit) get() = {}

val x: suspend @Foo () -> Unit get() = {}

val x: Comparable<@Foo suspend @Bar(10) @Foo () -> Unit> get() = {}

val x: Any = {} as @Foo suspend () -> Unit

fun foo(x: (suspend @Foo () -> Unit)) = x

fun foo(x: suspend @Foo(10) @Bar () -> Unit = { x: Int -> x }) {}

fun foo() {
    val x: @Foo suspend @Bar () -> Unit = {}
}

fun foo() {
    val x = { x: suspend @Foo () -> () -> Unit -> x }
}

abstract class A {
    abstract var x: @Foo suspend (suspend () -> (() -> Unit)) -> Int
}

fun foo(vararg x: @Foo @Bar(10) @Foo () -> Unit) = 10

fun foo(): @Foo.Bar suspend () -> Unit = {}

fun foo(): () -> @Foo.Bar suspend () -> Unit = {}

val x: Any get() = fun(): @Foo suspend () -> Unit {}

fun foo() {
    var x: (@Foo ()->suspend ()->Unit) -> Unit = {}
}

fun foo(x: Any) {
    if (x as @Foo @Bar(10) suspend @Foo () -> Unit is suspend @Foo @Bar(10) @Foo () -> Unit) {}
}

fun foo(y: Any) {
    var x = y as (@Foo suspend () -> () -> Unit) -> Unit
}
