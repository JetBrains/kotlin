// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
enum class E {
    entry
}

val Int.entry: Int get() = 42
val Long.entry: Int get() = 239

val e = E.entry

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
