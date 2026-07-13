// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

enum class MyKotlinEnum {
    FirstEntry, SecondEntry;
}

// MODULE: main(lib)
// FILE: main.kt
fun test() {
    MyKotlinEnum.val<caret>ueOf("")
}
