// RUN_PIPELINE_TILL: BACKEND
//KT-3038 Wrong type inference for enum entry
package a

enum class TestEnum {
    FIRST,
    SECOND
}

fun <T> inferenceTest(a: T) : T = a

fun hello() {
    var enumElemFirst = inferenceTest(TestEnum.FIRST)
    enumElemFirst = TestEnum.SECOND // Type mismatch: inferred type is testDebug.TestEnum.<class-object-for-TestEnum>.SECOND but testDebug.TestEnum.<class-object-for-TestEnum>.FIRST was expected

    var enumElemSecond : TestEnum = inferenceTest(TestEnum.FIRST)
    enumElemSecond = TestEnum.SECOND // Ok

    use(enumElemFirst, enumElemSecond)
}


fun use(vararg a: Any?) = a