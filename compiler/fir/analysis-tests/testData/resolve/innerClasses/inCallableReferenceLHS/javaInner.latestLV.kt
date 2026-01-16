// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE

// FILE: JavaOuter.java

public class JavaOuter<A> {
    public class JavaInner<B> {
        public void foo() {
        }

        static public void bar() {
        }
    }
}

// FILE: main.kt

fun main() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>JavaOuter<!>.JavaInner::foo
    JavaOuter<Int>.JavaInner<String>::foo
    JavaOuter<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>.JavaInner::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>JavaOuter<!>.JavaInner<Int, String>::foo

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>JavaOuter<!>.JavaInner::bar
    JavaOuter<Int>.JavaInner<String>::bar
    JavaOuter<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, String><!>.JavaInner::bar
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>JavaOuter<!>.JavaInner<Int, String>::bar
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration */
