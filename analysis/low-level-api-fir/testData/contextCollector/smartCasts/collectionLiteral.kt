// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
fun foo(x: Any, y: List<String>) { }

fun test(x: Any, y: Any) {
    foo(x, [y as String])
    <expr>y.length</expr>
}
