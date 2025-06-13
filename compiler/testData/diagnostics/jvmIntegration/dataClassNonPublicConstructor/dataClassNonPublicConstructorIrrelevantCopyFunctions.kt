// RUN_PIPELINE_TILL: BACKEND
// MODULE: lib
// KOTLINC_ARGS: -XXLanguage:+ErrorAboutDataClassCopyVisibilityChange -XXLanguage:+DataClassCopyRespectsConstructorVisibility
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange :+DataClassCopyRespectsConstructorVisibility
// FILE: Lib.kt
data class Data private constructor(val value: String)

fun copy(<!UNUSED_PARAMETER!>value<!>: String = ""): Data = null!!
class IrrelevantClass {
    fun copy(<!UNUSED_PARAMETER!>value<!>: String = ""): Data = null!!
}

// MODULE: main(lib)
// KOTLINC_ARGS: -progressive -XXLanguage:+ErrorAboutDataClassCopyVisibilityChange -XXLanguage:+DataClassCopyRespectsConstructorVisibility
// FILE: main.kt
fun test(irrelevantClass: IrrelevantClass) {
    copy()
    irrelevantClass.copy()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, data, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
