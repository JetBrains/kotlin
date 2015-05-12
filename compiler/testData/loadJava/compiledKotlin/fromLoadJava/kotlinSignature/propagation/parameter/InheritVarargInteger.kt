package test

public interface InheritVarargInteger {

    public interface Super {
        public fun foo(vararg p0: Int?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(vararg p0: Int?)
    }
}
