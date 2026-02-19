// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB
data class Data<out T> private constructor(val t: T) {
    inline fun foo(other: Data<String>) {
        other.copy()
        copy()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, inline, nullableType, out, primaryConstructor,
propertyDeclaration, typeParameter */
