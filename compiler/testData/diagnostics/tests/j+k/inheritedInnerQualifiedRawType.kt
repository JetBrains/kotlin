// RUN_PIPELINE_TILL: BACKEND
// Raw-ness of a qualified inner-class reference follows the qualifier written in source, not the
// declaring outer of the resolved class: `Sub.Inner` denotes `Outer<String>.Inner` and is not raw
// (javac reports no `rawtypes` warning for it), while `Outer.Inner` written in the very same place is.
// A walk over the declaring outer chain alone reports both raw, erasing `String` away and
// contradicting the type arguments the same reference recovers from the supertype hierarchy.
// A raw type is silently compatible with anything, so the two are told apart through erasure of the
// inner class's members: only a raw `Inner` accepts an `Int` where `T` stands.

// FILE: Outer.java
public class Outer<T> {
    public class Inner {
        public T get() { return null; }
        public void put(T t) {}
    }
}

// FILE: Sub.java
public class Sub extends Outer<String> {
    public Sub.Inner viaSubclass() { return null; }
    public Outer.Inner viaDeclaringOuter() { return null; }
}

// FILE: test.kt
fun consumeString(s: String) {}

fun test(s: Sub) {
    consumeString(s.viaSubclass().get())
    s.viaSubclass().put("")

    s.viaDeclaringOuter().put(1)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, javaType, stringLiteral */
