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
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<List<*>, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<*, *><!>>()
    a.value<<!UPPER_BOUND_VIOLATED!>BodySpec<Int, *><!>>()
    a.value()

    k.bar<<!UPPER_BOUND_VIOLATED!>WithDnn<*><!>>()
    k.bar()

    j.value<<!UPPER_BOUND_VIOLATED!>JavaClass<*,*><!>>()
    j.value()
}

interface BodySpec<B, S : BodySpec<B, S>> {
    fun <T : S> value(): T
}

interface WithDnn<T : WithDnn<T>?> {
    fun <K : T & Any> bar() {}
}