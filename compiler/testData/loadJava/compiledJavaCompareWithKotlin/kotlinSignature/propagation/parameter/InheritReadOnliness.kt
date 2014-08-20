package test

public trait InheritReadOnliness {

    public trait Super {
        public fun foo(p: List<String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p: List<String>)
    }
}
