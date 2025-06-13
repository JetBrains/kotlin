// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
@ExposedCopyVisibility
data class Data private constructor(val x: Int)

fun usage(data: Data) {
    data.copy()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration */
