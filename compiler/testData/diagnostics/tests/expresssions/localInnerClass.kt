// RUN_PIPELINE_TILL: BACKEND
interface Foo

fun bar() {
    object : Foo {
        fun foo(): Foo {
            return Derived(42)
        }

        inner class Derived(val x: Int) : Foo
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, integerLiteral,
interfaceDeclaration, localClass, primaryConstructor, propertyDeclaration */
