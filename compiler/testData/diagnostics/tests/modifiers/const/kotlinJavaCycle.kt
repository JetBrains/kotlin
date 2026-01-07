// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: Bar.java

public class Bar {
    public static final int BAR = Foo.FOO + 1;
}

// FILE: Test.kt

class Foo {
    companion object {
        const val FOO = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Baz.BAZ + 1<!>
    }
}

class Baz {
    companion object {
        const val BAZ = Bar.BAR + 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, const, integerLiteral, javaProperty,
objectDeclaration, propertyDeclaration */
