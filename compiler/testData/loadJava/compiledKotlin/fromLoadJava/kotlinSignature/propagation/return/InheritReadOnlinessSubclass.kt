package test

public interface InheritReadOnlinessSubclass {

    public interface Super {
        public fun foo(): Collection<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): List<String>
    }
}
