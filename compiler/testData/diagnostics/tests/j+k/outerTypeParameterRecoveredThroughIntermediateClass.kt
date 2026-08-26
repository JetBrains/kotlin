// RUN_PIPELINE_TILL: FRONTEND
// The outer argument of the inherited `Inner` is recovered from `Sub`'s supertype clause through
// `Mid`, which substitutes `A`'s `T` into `Outer`. That parameter belongs to the *outer* class, not
// to `Sub` itself, and it is not `Sub`'s own `U` — javac agrees (`incompatible types: T cannot be
// converted to U`).

// FILE: Outer.java
public class Outer<E> {
    public class Inner {
        public E get() { return null; }
    }
}

// FILE: Mid.java
public class Mid<X> extends Outer<X> {}

// FILE: A.java
public class A<T> {
    public class Sub<U> extends Mid<T> {
        public Inner foo() { return null; }
    }
}

// FILE: test.kt
fun consumeInt(i: Int) {}
fun consumeString(s: String) {}

fun test(s: A<Int>.Sub<String>) {
    consumeInt(s.foo().get())
    consumeString(<!ARGUMENT_TYPE_MISMATCH!>s.foo().get()<!>)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType */
