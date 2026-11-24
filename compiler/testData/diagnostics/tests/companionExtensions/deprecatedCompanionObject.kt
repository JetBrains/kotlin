// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FIR_DUMP

// FILE: deprecations.kt
package p

@RequiresOptIn
annotation class Experimental

class A {
    @Deprecated("", level = DeprecationLevel.ERROR)
    @Experimental
    companion object

    class Nested {
        @Experimental
        @Deprecated("", level = DeprecationLevel.WARNING)
        companion object
    }
}

class B {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    companion object
}

companion fun A.foo() = Unit
companion val A.bar get() = Unit
companion operator fun A.invoke(x: Int) = Unit

companion fun A.test() {
    foo()
    bar

    ::foo
    ::bar
}

companion fun B.foo() = Unit
companion val B.bar get() = Unit

companion fun B.test() {
    foo()
    bar

    ::foo
    ::bar
}

companion fun A.Nested.foo() = Unit

fun test() {
    <!DEPRECATION_ERROR, OPT_IN_USAGE_ERROR!>A<!>.foo()
    <!DEPRECATION_ERROR, OPT_IN_USAGE_ERROR!>A<!>.bar
    <!DEPRECATION_ERROR, OPT_IN_USAGE_ERROR!>A<!>(42)
    <!DEPRECATION_ERROR!>B<!>.foo()
    <!DEPRECATION_ERROR!>B<!>.bar
    A.<!DEPRECATION, OPT_IN_USAGE_ERROR!>Nested<!>.foo()

    <!DEPRECATION_ERROR, OPT_IN_USAGE_ERROR!>A<!>::foo
    <!DEPRECATION_ERROR, OPT_IN_USAGE_ERROR!>A<!>::bar
    <!DEPRECATION_ERROR!>B<!>::foo
    <!DEPRECATION_ERROR!>B<!>::bar
    A.<!DEPRECATION, OPT_IN_USAGE_ERROR!>Nested<!>::foo
}

// FILE: enumInitializer.kt
package q

enum class E(val x: Int) {
    A(<!UNINITIALIZED_ENUM_COMPANION!>E<!>.init);
    companion object {}
}

companion val E.init = 1

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, companionObject, enumDeclaration,
enumEntry, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, nestedClass, objectDeclaration,
operator, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
