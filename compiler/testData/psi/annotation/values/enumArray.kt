// FILE: Arrays.kt
annotation class Arrays(val value: Array<MyEnum>)

// FILE: MyEnum.kt
enum class MyEnum {
    ENTRY1, ENTRY2;
}

// FILE: WithArrays.kt
import MyEnum.ENTRY2

@Arrays([MyEnum.ENTRY1, ENTRY2])
class WithArrays
