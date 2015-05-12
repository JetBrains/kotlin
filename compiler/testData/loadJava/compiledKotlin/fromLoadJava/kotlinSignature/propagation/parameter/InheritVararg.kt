package test

public interface InheritVararg {

    public interface Super {
        public fun foo(vararg p0: String?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(vararg p0: String?)
    }
}
