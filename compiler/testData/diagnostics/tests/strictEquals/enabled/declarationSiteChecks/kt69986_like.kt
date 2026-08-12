// RUN_PIPELINE_TILL: BACKEND

// FILE: Foo.kt

open class Foo {
    open class Bar
}

// FILE: q/test.kt

package q

class Baz : Foo.Bar() {
    override fun equals(@EqualityBound(Foo.Bar::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nestedClass, nullableType, operator,
override */
