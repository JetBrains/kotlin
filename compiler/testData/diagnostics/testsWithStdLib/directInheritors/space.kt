// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib
// FILE: Lib.kt
interface MyDataItem {
    val id: String
    val text1: String
    val text2: String
}

// MODULE: main(lib)
// FILE: Main.kt
fun createItemVM() = object {
    inner class MyItemVM(data: MyDataItem) : MyDataItem by data {
        val isSelected = false
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inheritanceDelegation, inner,
interfaceDeclaration, localClass, primaryConstructor, propertyDeclaration */
