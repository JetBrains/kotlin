// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74926

enum class Enum(val first: Any?, val second: Any?) {
    ENTRY(<!UNINITIALIZED_ENUM_COMPANION_REFERENCE!>Companion<!>, <!UNINITIALIZED_ENUM_COMPANION!>Companion<!>.member());

    companion object {
        fun member() {}
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, functionDeclaration, nullableType, objectDeclaration,
primaryConstructor, propertyDeclaration */
