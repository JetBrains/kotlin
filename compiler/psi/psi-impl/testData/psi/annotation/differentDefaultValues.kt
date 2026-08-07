// FILE: Anno.kt

import kotlin.reflect.KClass

annotation class Anno(
    val first: MyEnum = MyEnum.Entry2,
    val second: KClass<*> = MyEnum::class,
    val third: Int = 1 + 2,
)

// FILE: MyEnum.kt
enum class MyEnum {
    Entry1,
    Entry2,
}
