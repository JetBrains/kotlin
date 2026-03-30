// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ReferencesToSyntheticJavaProperties

// FILE: Foo.java
public class Foo extends Base {
}

// FILE: Main.kt
open class Base {
    open val foo: Int = 904
}

val prop = Foo::foo

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, integerLiteral, javaType, propertyDeclaration */
