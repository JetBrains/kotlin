// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-84185
// JDK_KIND: FULL_JDK_21
//   ^ to allow static member in inner class in Java
// LANGUAGE: -ForbidUselessTypeArgumentsIn25

// FILE: JavaUtils.java

public class JavaUtils<T> {
    public static void foo() { return; }
    public static String bar;

    public static class Nested {
        public static void foo() { return; }
    }

    public class Inner {
        public static void foo() { return; }
    }
}

// FILE: main.kt

enum class E {
    X;
}

fun test() {
    E<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.X
    E<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int, String><!>.X

    E<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.values()
    E<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int, String><!>.valueOf("E")

    JavaUtils<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><String><!>.foo()
    JavaUtils<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Nothing><!>.bar
    JavaUtils.Nested<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Any?><!>.foo()
    JavaUtils<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.Inner.foo()
    JavaUtils<Int>.Inner<!TYPE_ARGUMENTS_NOT_ALLOWED_WARNING!><Int><!>.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, lambdaLiteral, nullableType,
stringLiteral, typeParameter */
