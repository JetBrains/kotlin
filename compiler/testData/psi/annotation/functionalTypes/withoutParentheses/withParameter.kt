// Issue: KT-31734

val x: (@Foo suspend (Int) -> Unit) get() = {}

val x: @Foo ((x: @Foo (@Foo () -> Int) -> Int) -> Unit) get() = {}

val x: suspend @Foo (x: @Foo (@Foo (x: kotlin.Any) -> Int) -> Int) -> Unit get() = {}

val x: Comparable<@Foo @Bar(10) @Foo (x: kotlin.Any = {}) -> Unit> get() = {}

val x: Any = {} as @Foo suspend (x: @Foo Foo) -> (y: @Foo Bar) -> Unit

fun foo(x: (@Foo (@Foo kotlin.Any)->()->Unit)) = x

fun foo(x: suspend @Foo(10) @Bar (kotlin.Any) -> Unit = { x: Int -> x }) {}

fun foo() {
    val x: @Foo suspend @Bar (Coomparable<kotlin.Any>) -> Unit = {}
}

fun foo() {
    val x = { x: suspend @Foo (Coomparable<@Foo @Bar(10) @Foo () -> Unit>) -> () -> Unit -> x }
}

abstract class A {
    abstract var x: @Foo suspend (suspend (Int) -> ((Int) -> Unit)) -> Int
}

fun foo(vararg x: @Foo @Bar(10) @Foo (Any) -> Unit) = 10

fun foo(): @Foo.Bar suspend (Nothing) -> Unit = {}

fun foo(): () -> @Foo.Bar suspend (Bar) -> Unit = {}

val x: Any get() = fun(): @Foo (Coomparable<Nothing>) -> Unit {}

fun foo() {
    var x: (@Foo (()->Unit)-> ()->Unit) -> Unit = {}
}

fun foo(x: Any) {
    if (x as @Foo @Bar(10) @Foo (()->Unit) -> Unit is suspend @Foo @Bar(10) @Foo (()->Unit) -> Unit) {}
}

fun foo(y: Any) {
    var x = y as (@Foo suspend (()->Unit) -> (()->Unit) -> Unit) -> Unit
}
