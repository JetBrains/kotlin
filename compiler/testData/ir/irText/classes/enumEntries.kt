// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB

enum class MyEnum {
    Ok, Nope
}

fun box() = MyEnum.entries
