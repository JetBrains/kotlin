// Issue: KT-31734

val x: (@[Foo] suspend (Int) -> Unit) get() = {}

val x: (@[Foo] () -> Unit) get() = {}

val x: (@[Foo] suspend () -> Unit) get() = {}

val x: @[Foo] ((x: @[Foo] (@[Foo Foo] () -> Int) -> Int) -> Unit) get() = {}

val x: suspend @[Foo] (x: @[Foo Foo] (@[Foo Foo] (x: kotlin.Any) -> Int) -> Int) -> Unit get() = {}

val x: Comparable<@[Foo] @[Bar(10)] @[Foo] () -> Unit> get() = {}

val x: Any = {} as @[Foo Foo] suspend (x: @[Foo] Foo) -> (y: @[Foo] Bar) -> Unit

fun foo(x: (@Foo ()->()->Unit)) = x

fun foo(x: suspend @[Foo Foo(10)] @[Bar] (kotlin.Any) -> Unit = { x: Int -> x }) {}

fun foo() {
    val x: @[Foo(10)] @[Bar] (Coomparable<kotlin.Any>) -> Unit = {}
}

fun foo() {
    val x = { x: suspend @[Foo Bar] (Coomparable<@[Foo Bar Bar Bar Bar Bar] @[Bar(10)] @[Foo Bar] () -> Unit>) -> () -> Unit -> x }
}

abstract class A {
    abstract var x: @[Foo Bar] suspend (() -> ((Int) -> Unit)) -> Int
}

fun foo(vararg x: @[Foo Bar] @[Bar(10)] @[Foo Bar] () -> Unit) = 10

fun foo(): @[Foo.Bar Foo.Bar(1)] suspend () -> Unit = {}

fun foo(): () -> @[Foo.Bar] () -> Unit = {}

val x: Any get() = fun(): @[Foo()] (Coomparable<Nothing>) -> Unit {}

fun foo() {
    var x: (@[Foo Bar] (()->Unit)-> ()->Unit) -> Unit = {}
}

fun foo(x: Any) {
    if (x as @[Foo] @[Bar(10) Bar] @[Foo Bar] (()->Unit) -> Unit is suspend @[Foo] @[Bar (10)] @[Foo Bar (10)] (()->Unit) -> Unit) {}
}

fun foo(y: Any) {
    var x = y as (@[Foo Bar] suspend (()->Unit) -> (()->Unit) -> Unit) -> Unit
}
