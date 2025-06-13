// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FILE: Test.java
public class Test {
    public <T> T with(Foo<T> matcher) {
        return null;
    }
    public boolean with(Foo<Boolean> matcher) {
        return false;
    }
}

// FILE: main.kt
class Foo<T>
fun main(foo1: Foo<Boolean>, foo2: Foo<String>) {
    val x = object : Test() {} // FE exception is thrown here

    x.with(foo1) checkType { _<Boolean>() }
    x.with(foo2) checkType { _<String>() }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, flexibleType, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, javaFunction, javaType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter, typeWithExtension */
