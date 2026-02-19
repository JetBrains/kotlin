// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=check
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
@IgnorableReturnValue fun ignorableTopLvl(): String = ""

// FILE: markedTopLevel.kt
@file:MustUseReturnValues

fun markedTopFunction(): String = ""
val markedTopProperty get() = 25

// FILE: Marked.kt
@MustUseReturnValues
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
