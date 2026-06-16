// RUN_PIPELINE_TILL: FRONTEND

fun wrongParameter(<!UNSUPPORTED!>@EqualityBound(String::class)<!> other: Any?) = Unit

<!WRONG_ANNOTATION_TARGET!>@EqualityBound(String::class)<!>
fun wrongTarget() = Unit

class Wrong {
    operator fun get(<!UNSUPPORTED!>@EqualityBound(String::class)<!> other: Any?) = Unit
    fun equals(<!UNSUPPORTED!>@EqualityBound(String::class)<!> other: CharSequence): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator */
