package test

public trait InheritReadOnlinessSubclass {

    public trait Super {
        public fun foo(): Collection<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): List<String>
    }
}
