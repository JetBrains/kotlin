// RUN_PIPELINE_TILL: CODEGEN
val list: List<String>
    field = mutableListOf<String>()

fun add(s: String) {
    list.add(s)
}

/* GENERATED_FIR_TAGS: explicitBackingField, functionDeclaration, propertyDeclaration, smartcast */
