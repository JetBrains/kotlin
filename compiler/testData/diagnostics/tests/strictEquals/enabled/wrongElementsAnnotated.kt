// RUN_PIPELINE_TILL: FRONTEND

fun wrongParameter(<!UNSUPPORTED!>@RestrictedTo(String::class)<!> other: Any?) = Unit

<!WRONG_ANNOTATION_TARGET!>@RestrictedTo(String::class)<!>
fun wrongTarget() = Unit

class Wrong {
    operator fun get(<!UNSUPPORTED!>@RestrictedTo(String::class)<!> other: Any?) = Unit
    fun equals(<!UNSUPPORTED!>@RestrictedTo(String::class)<!> other: CharSequence): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator */
