// TARGET_BACKEND: JVM_IR
// AFTER KT-53649 - TARGET_BACKEND: NATIVE, JS_IR
// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: -EnumEntries
// FILE: MyEnums.kt
enum class MyEnum {
    N, O
}

enum class MyEnum2 {
    O, K
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries
// FILE: Box.kt

@OptIn(ExperimentalStdlibApi::class)
object Container {
    fun getK() = MyEnum2.entries[1].toString()
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString() + Container.getK()
}
