// "Cast expression 'Foo<Number>()' to 'Foo<Int>'" "false"
// ACTION: Change 'foo' function return type to 'Foo<Number>'
// ACTION: Convert to expression body
// ERROR: Type mismatch: inferred type is Foo<Number> but Foo<Int> was expected
class Foo<T>

fun foo(): Foo<Int> {
    return <caret>Foo<Number>()
}