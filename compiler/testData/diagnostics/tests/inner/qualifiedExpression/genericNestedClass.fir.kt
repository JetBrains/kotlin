// NI_EXPECTED_FILE
class Outer {
    class Nested<T>
}

fun nested() = Outer.Nested<Int>()
fun noArguments() = Outer.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Nested<!>()
fun noArgumentsExpectedType(): Outer.Nested<String> = Outer.Nested()
fun manyArguments() = Outer.<!INAPPLICABLE_CANDIDATE!>Nested<!><String, Int>()
