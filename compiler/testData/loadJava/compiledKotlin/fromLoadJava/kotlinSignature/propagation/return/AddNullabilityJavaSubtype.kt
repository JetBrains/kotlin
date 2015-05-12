package test

public interface AddNullabilityJavaSubtype {

    public interface Super {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): String
    }
}
