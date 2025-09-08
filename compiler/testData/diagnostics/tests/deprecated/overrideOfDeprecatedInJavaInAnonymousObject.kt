// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80399
// FILE: Base.java
public class Base {
  @Deprecated
  public void foo() {}
}

// FILE: main.kt
class Derived : Base() {
    @Deprecated("Deprecated in Java")
    override fun foo() {}
}

fun test() {
    val newClass = object : Base() {
        @Deprecated("Deprecated in Java")
        override fun foo() {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, javaType, localProperty,
override, propertyDeclaration, stringLiteral */
