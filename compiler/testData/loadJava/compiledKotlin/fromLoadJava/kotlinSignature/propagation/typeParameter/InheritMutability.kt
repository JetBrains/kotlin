package test

public interface InheritMutability {

    public interface Super {
        public fun <A: MutableList<String>> foo(a: A)
    }

    public interface Sub: Super {
        override fun <B: MutableList<String>> foo(a: B)
    }
}
