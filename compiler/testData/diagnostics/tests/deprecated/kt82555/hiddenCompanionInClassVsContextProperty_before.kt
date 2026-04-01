// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
//  ^ K1 is ignored
// LANGUAGE: +ContextParameters, -SkipHiddenObjectsInResolution
// ISSUE: KT-82555
// FIR_DUMP

object Outer {
    enum class <!REDECLARATION!>Nested<!> {
        ENTRY;

        object NestedMost

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        companion object
    }

    context(str: String)
    val <!REDECLARATION!>Nested<!>: Int get() = 42
}

fun test() {
    with("") {
        val ref = Outer.Nested::toString // must resolve to class
        val classRef = Outer.Nested::class // must resolve to class
        Outer.<!DEPRECATION_ERROR!>Nested<!> // must resolve to property
        Outer.Nested.valueOf("ENTRY") // must resolve to class
        Outer.Nested.ENTRY // must resolve to class
        Outer.Nested.NestedMost // must resolve to class
    }
}

/* GENERATED_FIR_TAGS: callableReference, classReference, companionObject, enumDeclaration, functionDeclaration, getter,
integerLiteral, lambdaLiteral, localProperty, nestedClass, objectDeclaration, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral */
