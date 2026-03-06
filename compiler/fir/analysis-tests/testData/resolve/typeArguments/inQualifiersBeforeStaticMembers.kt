// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-84185
// JDK_KIND: FULL_JDK_21
//   ^ to allow static member in inner class in Java

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
    E<Int>.X
    E<Int, String>.X

    E<Int>.values()
    E<Int, String>.valueOf("E")

    JavaUtils<String>.foo()
    JavaUtils<Nothing>.bar
    JavaUtils.Nested<Any?>.foo()
    JavaUtils<Int>.Inner.foo()
    JavaUtils<Int>.Inner<Int>.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, lambdaLiteral, nullableType,
stringLiteral, typeParameter */
