// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
class Outer {
    class Nested<T>
}

fun nested() = Outer.Nested<Int>()
fun noArguments() = Outer.Nested()
fun noArgumentsExpectedType(): Outer.Nested<String> = Outer.Nested()
fun manyArguments() = Outer.<!INAPPLICABLE_CANDIDATE!>Nested<!><String, Int>()
