// "Cast expression 'Foo<Number>()' to 'Foo<Int>'" "false"
// ACTION: Create test
// ERROR: Type mismatch: inferred type is Foo<kotlin.Number> but Foo<kotlin.Int> was expected
class Foo<T>

fun foo(): Foo<Int> {
    return Foo<Number>()
}