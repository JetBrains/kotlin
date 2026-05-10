// WITH_STDLIB

// MODULE: lib
// FILE: MyEnum.kt
enum class MyEnum {
    Nope, OK
}

// MODULE: main(lib)
// FILE: Box.kt
@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString()
}
