// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67825
// DUMP_INFERENCE_LOGS: MARKDOWN

// FILE: Test.kt
fun test(k: K<String>) {
    // In all three cases, we infer T = Nothing? and have a contradiction around JavaBox<out String> vs JavaBox<Nothing?>
    // However, K2 reports an error only in case with foo2 (but it should)
    k.foo(<!TYPE_MISMATCH!>JavaBox(null)<!>)
    foo2<String>(<!TYPE_MISMATCH!>JavaBox(null)<!>)
    foo3(<!TYPE_MISMATCH!>JavaBox(null)<!>)
}

class K<R> {
    fun foo(a: JavaBox<out R>) {}
}

fun <S> foo2(a: JavaBox<out S>) {}

fun foo3(a: JavaBox<out String>) {}

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) {
        a = b;
    }
    public T a;
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, nullableType, outProjection, typeParameter */
