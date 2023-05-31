// TARGET_BACKEND: JVM
package test

public interface TwoBounds {

    public interface Super {
        public fun <A: CharSequence> foo(a: A) where A: Cloneable
    }

    public interface Sub: Super {
        override fun <B: CharSequence> foo(a: B) where B: Cloneable
    }
}
