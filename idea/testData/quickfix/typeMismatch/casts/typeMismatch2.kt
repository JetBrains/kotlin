// "Cast expression 'Foo<Number>()' to 'Foo<Int>'" "false"
// ACTION: Create test
// ERROR: Type mismatch: inferred type is Foo<Number> but Foo<Int> was expected
class Foo<T>

fun foo(): Foo<Int> {
    return Foo<Number>()
}