//KT-3038 Wrong type inference for enum entry
package a

enum class TestEnum {
    FIRST,
    SECOND
}

fun inferenceTest<T>(a: T) : T = a

fun hello() {
    var enumElemFirst = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>inferenceTest(TestEnum.FIRST)<!>
    enumElemFirst = TestEnum.SECOND // Type mismatch: inferred type is testDebug.TestEnum.<class-object-for-TestEnum>.SECOND but testDebug.TestEnum.<class-object-for-TestEnum>.FIRST was expected

    var enumElemSecond : TestEnum = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>inferenceTest(TestEnum.FIRST)<!>
    enumElemSecond = TestEnum.SECOND // Ok

    use(enumElemFirst, enumElemSecond)
}


fun use(vararg a: Any?) = a