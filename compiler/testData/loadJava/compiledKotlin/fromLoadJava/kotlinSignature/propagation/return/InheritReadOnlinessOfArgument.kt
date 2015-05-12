package test

public interface InheritReadOnlinessOfArgument {

    public interface Super {
        public fun foo(): List<List<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): List<List<String>>
    }
}
