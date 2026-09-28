// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: -ReferencesToSyntheticJavaProperties

// FILE: Foo.java
public class Foo extends Base {
    @Override
    public int getFoo() {
        return super.getFoo();
    }
}

// FILE: Main.kt
open class Base {
    open val foo: Int = 904
}

val prop = Foo::foo

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, javaCallableReference, javaType, propertyDeclaration */
