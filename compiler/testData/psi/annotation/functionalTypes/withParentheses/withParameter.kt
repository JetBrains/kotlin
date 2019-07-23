// Issue: KT-31734

val x: (@Foo(10) suspend (Int) -> Unit) get() = {}

val x: @Foo({}) ((x: @Foo(10) (@Foo(11) () -> Int) -> Int) -> Unit) get() = {}

val x: suspend @Foo(10) (x: @Foo(10) (@Foo("") (x: kotlin.Any) -> Int) -> Int) -> Unit get() = {}

val x: Comparable<@Foo(10) @Bar(10) @Foo(listOf(10)) (x: kotlin.Any = {}) -> Unit> get() = {}

val x: Any = {} as @Foo({ x: Int -> 10}) suspend (x: @Foo(10) Foo) -> (y: @Foo(10) Bar) -> Unit

fun foo(x: (@Foo(10) (@Foo({ x: Int -> 10}) kotlin.Any)->()->Unit)) = x

fun foo(x: suspend @Foo(10) @Bar(10 + @Foo 3) (kotlin.Any) -> Unit = { x: Int -> x }) {}

fun foo() {
    val x: @Foo(10) suspend @Bar(throw Exception()) (Coomparable<kotlin.Any>) -> Unit = {}
}

fun foo() {
    val x = { x: suspend @Foo(null) (Coomparable<@Foo(10) @Bar(10) @Foo(10) () -> Unit>) -> () -> Unit -> x }
}

abstract class A {
    abstract var x: @Foo(10 as @Foo suspend (Int) -> ((Int) -> Unit)) suspend (suspend (Int) -> ((Int) -> Unit)) -> Int
}

fun foo(vararg x: @Foo(10) @Bar(10) @Foo(Any) (Any) -> Unit) = 10

fun foo(): @Foo.Bar(@Foo x) suspend (Nothing) -> Unit = {}

fun foo(): () -> @Foo.Bar('1') suspend (Bar) -> Unit = {}

val x: Any get() = fun(): @Foo("") (Coomparable<Nothing>) -> Unit {}

fun foo() {
    var x: (@Foo(object {}) (()->Unit)-> ()->Unit) -> Unit = {}
}

fun foo(x: Any) {
    if (x as @Foo({}) @Bar(10) @Foo(10) (()->Unit) -> Unit is suspend @Foo(10) @Bar(10) @Foo(10) (()->Unit) -> Unit) {}
}

fun foo(y: Any) {
    var x = y as (@Foo({}) suspend (()->Unit) -> (()->Unit) -> Unit) -> Unit
}
