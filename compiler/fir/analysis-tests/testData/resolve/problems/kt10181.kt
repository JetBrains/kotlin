// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-10181

// KT-10181: Bogus errors when enum entry name matches the enum name
enum class E(x: Int) {
    E(1)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, primaryConstructor */
