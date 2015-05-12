package test

public interface TwoSuperclasses {

    public interface Super1 {
        public fun <A: CharSequence> foo(a: A)
    }

    public interface Super2 {
        public fun <B: CharSequence> foo(a: B)
    }

    public interface Sub: Super1, Super2 {
        override fun <C: CharSequence> foo(a: C)
    }
}
