// !LANGUAGE: +IntrinsicConstEvaluation
// MODULE: m1-common
// FILE: common.kt
enum class MyEnum {
    FOO
}

annotation class Ann(val p: String)

@Ann("FOO")
expect fun matching()

@Ann("not FOO")
expect fun nonMatching()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(MyEnum.FOO.name)
actual fun matching() {}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@Ann(MyEnum.FOO.name)
actual fun nonMatching() {}<!>
