package test

public interface AddNullabilitySameGenericType2 {

    public interface Super {
        public fun foo(): MutableList<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): MutableList<String>
    }
}
