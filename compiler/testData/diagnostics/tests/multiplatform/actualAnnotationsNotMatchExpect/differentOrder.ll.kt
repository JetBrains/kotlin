// MODULE: m1-common
// FILE: common.kt
annotation class Ann1
annotation class Ann2

@Ann1
@Ann2
expect class AnnotationOrder

annotation class Ann3(vararg val numbers: Int)

@Ann3(1, 2)
expect class ValuesOrderInsideAnnotationArgument

annotation class Ann4(val arg1: String, val arg2: String)

@Ann4(arg1 = "1", arg2 = "2")
expect fun differentArgumentsOrder()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@Ann2
@Ann1
actual class AnnotationOrder

@Ann3(2, 1)
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ValuesOrderInsideAnnotationArgument<!>

@Ann4(arg2 = "2", arg1 = "1")
actual fun differentArgumentsOrder() {}
