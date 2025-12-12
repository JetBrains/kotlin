// LANGUAGE: +ExplicitBackingFields
// COMPILATION_ERRORS
// MODULE: original
val a: List<String>

// MODULE: copy
val a: List<String>
    field = mutableListOf<String>()
