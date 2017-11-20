// !WITH_NEW_INFERENCE
class Outer {
    class Nested<T>
}

fun nested() = Outer.Nested<Int>()
fun noArguments() = Outer.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Nested<!>()
fun noArgumentsExpectedType(): Outer.Nested<String> = Outer.Nested()
fun manyArguments() = Outer.Nested<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>()
