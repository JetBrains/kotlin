// RUN_PIPELINE_TILL: LOWERINGS
// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
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
    A.foo()
    A.bar
    A(42)
    B.foo()
    B.bar
    A.Nested.foo()

    A::foo
    A::bar
    B::foo
    B::bar
    A.Nested::foo
}

// FILE: enumInitializer.kt
package q

enum class E(val x: Int) {
    A(E.init);
    companion object {}
}

companion val E.init = 1

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, companionObject, enumDeclaration,
enumEntry, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, nestedClass, objectDeclaration,
operator, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
