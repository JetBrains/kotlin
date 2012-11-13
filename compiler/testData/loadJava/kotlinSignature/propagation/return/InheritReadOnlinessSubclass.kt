package test

public trait InheritReadOnlinessSubclass: Object {

    public trait Super: Object {
        public fun foo(): Collection<String>
    }

    public trait Sub: Super {
        override fun foo(): List<String>
    }
}
