// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT

private class Foo {
    fun method() {}
}

public interface I {
    public fun bar()
}

public fun f() {
    val i = object : I {
        internal var foo = 0
        override fun bar() {}
    }
    i.foo = 1

    class LocalClass {
        internal var foo = 0
    }
    LocalClass().foo = 1
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, classDeclaration, functionDeclaration, integerLiteral,
interfaceDeclaration, localClass, localProperty, override, propertyDeclaration */
