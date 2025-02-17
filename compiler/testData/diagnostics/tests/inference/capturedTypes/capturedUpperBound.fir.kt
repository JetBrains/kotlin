// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-65712
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: JavaClass.java
public interface JavaClass<B, S extends JavaClass<B, S>> {
    public default <T extends S> T value() {
        return null;
    }
}

// FILE: test.kt
fun test(a: BodySpec<List<*>, *>, k: WithDnn<*>, j: JavaClass<List<*>, *>) {
    a.value<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>BodySpec<List<*>, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>BodySpec<*, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>BodySpec<Int, *><!>>()
    a.value()

    k.bar<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>WithDnn<*><!>>()
    k.bar()

    j.value<<!UPPER_BOUND_VIOLATED_CAPTURED_TYPE!>JavaClass<*,*><!>>()
    j.value()
}

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> value(): T
}

interface WithDnn<T : WithDnn<T>?> {
    fun <K : T & Any> bar() {}
}
