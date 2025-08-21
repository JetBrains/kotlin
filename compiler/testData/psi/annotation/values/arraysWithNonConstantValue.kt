// COMPILATION_ERRORS
// FILE: Arrays.kt
annotation class Arrays(val value: IntArray)

// FILE: MyEnum.kt
enum class MyEnum {
    ENTRY;
}

// FILE: WithArrays.kt
@Arrays(
    [1, MyEnum.ENTRY, fun a() {}, bar.baz?.foo()]
)
class WithArrays
