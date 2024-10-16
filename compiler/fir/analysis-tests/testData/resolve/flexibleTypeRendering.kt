// FILE: main.kt

fun foo(p: AAA<String>, s: Int?) {
    p.<!CANNOT_INFER_PARAMETER_TYPE!>process<!>(<!ARGUMENT_TYPE_MISMATCH("Q! (of fun <Q : String!> process); kotlin.Int?")!>s<!>)
}

// FILE: AAA.java
public class AAA<P> {
    public final <Q extends P> void process(Q q) {}
}
