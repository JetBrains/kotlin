// RUN_PIPELINE_TILL: BACKEND
// JLS 4.6/8.1.3: `A.Middle.Inner<String>` is raw only if a type parameter which is actually in
// scope for the reference is left without an argument. `Middle` is `static`, so it inherits none of
// `A`'s parameters and `A`'s `T` is not in scope below it — the reference is not raw, even though an
// outer *declaration* in the chain is generic. A walk over the enclosing classes which ignores
// `static` reports it raw and erases `String` to `Any`.
// A raw type is flexible and therefore silently compatible with anything, so this is asserted on the
// rendered types, not on diagnostics.

// FILE: A.java
public class A<T> {
    public static class Middle {
        public class Inner<X> {
            public X x;
        }
    }

    public class Sub<Y> {
        public Y y;
    }
}

// FILE: Holder.java
public class Holder {
    public A.Middle.Inner<String> throughStatic() { return null; }
    public A.Middle.Inner throughStaticRaw() { return null; }
    public A<CharSequence>.Sub<String> throughNonStatic() { return null; }
    public A.Sub throughNonStaticRaw() { return null; }
}

// FILE: test.kt
fun test(h: Holder) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(A.Middle.Inner<(kotlin.String..kotlin.String?)>..A.Middle.Inner<(kotlin.String..kotlin.String?)>?)")!>h.throughStatic()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(A.Middle.Inner<(kotlin.Any..kotlin.Any?)>..A.Middle.Inner<*>?)")!>h.throughStaticRaw()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(A.Sub<(kotlin.String..kotlin.String?), (kotlin.CharSequence..kotlin.CharSequence?)>..A.Sub<(kotlin.String..kotlin.String?), (kotlin.CharSequence..kotlin.CharSequence?)>?)")!>h.throughNonStatic()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(A.Sub<(kotlin.Any..kotlin.Any?), (kotlin.Any..kotlin.Any?)>..A.Sub<*, *>?)")!>h.throughNonStaticRaw()<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
