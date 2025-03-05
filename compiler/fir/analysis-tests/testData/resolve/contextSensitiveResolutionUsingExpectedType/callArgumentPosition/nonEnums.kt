// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JavaClass.java
public class JavaClass {
    public static JavaClass INSTANCE = null;

    public static void expectJavaClass(JavaClass j) {}
}

// FILE: main.kt

sealed class MyClass {
    companion object {
        val FOO: MyClass = TODO()
        val FOR_TYPE_MISMATCH = ""
    }

    object Bar : MyClass()
    object ForTypeMismatch
}

fun expectInt(x: Int) {}

fun expectMyClass(x: MyClass) {}

fun <X> id(x: X): X = TODO()

fun MyClass.foo() {
    expectInt(MAX_VALUE)

    expectMyClass(FOO)
    expectMyClass(id(FOO))

    expectMyClass(<!ARGUMENT_TYPE_MISMATCH!>FOR_TYPE_MISMATCH<!>)
    expectMyClass(id(<!ARGUMENT_TYPE_MISMATCH!>FOR_TYPE_MISMATCH<!>))

    expectMyClass(Bar)
    expectMyClass(id(Bar))

    expectMyClass(<!ARGUMENT_TYPE_MISMATCH!>ForTypeMismatch<!>)
    expectMyClass(id(<!ARGUMENT_TYPE_MISMATCH!>ForTypeMismatch<!>))

    JavaClass.expectJavaClass(INSTANCE)
}
