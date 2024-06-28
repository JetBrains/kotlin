// TARGET_BACKEND: JVM
package test

public interface TwoTypeParameters {

    public interface Super {
        public fun <A: CharSequence, B: Cloneable> foo(a: A, b: B)
    }

    public interface Sub: Super {
        override fun <B: CharSequence, A: Cloneable> foo(a: B, b: A)
    }
}
