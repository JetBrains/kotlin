// RUN_PIPELINE_TILL: CODEGEN
// FILE: A.java

public class A {
    public String getFoo() {
        return "Foo";
    }
}

// FILE: B.kt

class B(private val foo: String) : A() {
    override fun getFoo(): String = foo
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, override, primaryConstructor,
propertyDeclaration */
