// RUN_PIPELINE_TILL: CODEGEN
// MODULE: lib
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange :+DataClassCopyRespectsConstructorVisibility
// FILE: Lib.kt
data class Data private constructor(val value: String)

fun copy(value: String = ""): Data = null!!
class IrrelevantClass {
    fun copy(value: String = ""): Data = null!!
}

// MODULE: main(lib)
// FILE: main.kt
fun test(irrelevantClass: IrrelevantClass) {
    copy()
    irrelevantClass.copy()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
