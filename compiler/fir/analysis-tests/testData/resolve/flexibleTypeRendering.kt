// FILE: main.kt

fun foo(p: AAA<String>, s: Int?) {
    p.process(<!ARGUMENT_TYPE_MISMATCH("Q!; kotlin.Int?")!>s<!>)
}

// FILE: AAA.java
public class AAA<P> {
    public final <Q extends P> void process(Q q) {}
}
