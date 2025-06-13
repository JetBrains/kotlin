// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -EnumEntries, -PrioritizedEnumEntries
// WITH_STDLIB

package pckg

enum class A {
    ;

    companion object
}

val <T> T.entries: Int get() = 0

fun test() {
    val i: Int = <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>
    A.Companion.entries
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, functionDeclaration, getter, integerLiteral, localProperty,
nullableType, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
