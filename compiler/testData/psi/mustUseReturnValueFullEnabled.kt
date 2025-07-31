// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// FILE: Unmarked.kt
class Unmarked {
    fun getStuff(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }

    @IgnorableReturnValue fun ignorable(): String = ""
}

// FILE: unmarkedTopLevel.kt
fun unmarkedTopFunction(): String = ""
val unmarkedTopProperty get() = 25

// FILE: markedTopLevel.kt
@file:MustUseReturnValue

fun markedTopFunction(): String = ""
val markedTopProperty get() = 25
@IgnorableReturnValue fun ignorableTopLvl(): String = ""

// FILE: Marked.kt
@MustUseReturnValue
class Marked {
    fun alreadyApplied(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }

    @IgnorableReturnValue fun ignorable(): String = ""
}

// FILE: MyEnum.kt
enum class MyEnum {
    A, B;
    fun foo() = ""
}
