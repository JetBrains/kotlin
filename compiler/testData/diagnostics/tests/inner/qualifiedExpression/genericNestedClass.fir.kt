// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE
class Outer {
    class Nested<T>
}

fun nested() = Outer.Nested<Int>()
fun noArguments() = Outer.<!CANNOT_INFER_PARAMETER_TYPE!>Nested<!>()
fun noArgumentsExpectedType(): Outer.Nested<String> = Outer.Nested()
fun manyArguments() = Outer.Nested<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, nullableType, typeParameter */
