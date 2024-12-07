// WITH_STDLIB

interface Foo

fun test(obj: Any) {
    require(obj is Foo)
    <expr>consume(obj)</expr>
}

fun consume(obj: Any) {}