// FULL_JDK
// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM

enum class MyEnum {
    Ok, Nope
}

fun box() = MyEnum.entries
