package test

public trait InheritReadOnlinessOfArgument {

    public trait Super {
        public fun foo(): List<List<String>>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): List<List<String>>
    }
}
