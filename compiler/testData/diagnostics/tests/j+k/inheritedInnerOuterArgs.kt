// RUN_PIPELINE_TILL: BACKEND
// `Inner<String>` inside `Derived<E>` denotes `Base<Integer, E>.Inner<String>`: the implicit outer
// type arguments come from the *inherited* supertype, not from `Base`'s own declaration, whose
// parameters `H1`/`H2` are not in scope at that point. A Java view which emits the declared
// parameters produces unresolved type arguments; the resulting error types are silently compatible
// with everything, so the substitution is asserted on the rendered types rather than on diagnostics.

// FILE: Base.java
public class Base<H1, H2> {
    public class Inner<H3> {
        public H1 first() { return null; }
        public H2 second() { return null; }
        public H3 third() { return null; }
    }
}

// FILE: Derived.java
public class Derived<E> extends Base<Integer, E> {
    public Inner<String> makeInner() { return null; }
}

// FILE: test.kt
fun consumeInt(i: Int) {}
fun consumeCharSequence(c: CharSequence) {}
fun consumeString(s: String) {}

fun test(d: Derived<CharSequence>) {
    consumeInt(d.makeInner().first())
    consumeCharSequence(d.makeInner().second())
    consumeString(d.makeInner().third())

    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Int..kotlin.Int?)")!>d.makeInner().first()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.CharSequence..kotlin.CharSequence?)")!>d.makeInner().second()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>d.makeInner().third()<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType */
