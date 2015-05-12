package test

public interface NotNullToNullable {

    public interface Super {
        public fun foo(p0: String)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p0: String)
    }
}
