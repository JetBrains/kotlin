// FILE: AnnoWithDefaultName.kt
annotation class AnnoWithDefaultName(val value: Int)

// FILE: AnnoWithCustomName.kt
annotation class AnnoWithCustomName(val string: String)

// FILE: AnnoWithCustomNameAndDefaultValue.kt
annotation class AnnoWithCustomNameAndDefaultValue(val string: String = "str")

// FILE: AnnoWithDefaultNameAndDefaultValue.kt
annotation class AnnoWithDefaultNameAndDefaultValue(val value: Int = 1)

// FILE: AnnoWithVararg.kt
public annotation class AnnoWithVararg(
    vararg val value: Int
)

// FILE: AnnoWithVarargAndDefaultVakue.kt
public annotation class AnnoWithVarargAndDefaultVakue(
    vararg val value: Int = [0]
)
