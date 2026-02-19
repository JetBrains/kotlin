// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
data class Data private constructor(val x: Int) {
    fun copy() = Data(1)
}

fun local(data: Data) {
    data.copy()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
