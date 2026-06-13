// RUN_PIPELINE_TILL: BACKEND
// FILE: AbstractClass.java

public abstract class AbstractClass {
    public static class StaticClass {

    }
}

// FILE: User.kt

class User : AbstractClass() {
    fun foo() {
        val sc = StaticClass()
    }
}

fun test() {
    AbstractClass.StaticClass()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType, localProperty, propertyDeclaration */
