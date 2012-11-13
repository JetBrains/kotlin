package test

public trait InheritReadOnlinessOfArgument: Object {

    public trait Super: Object {
        public fun foo(): List<List<String>>
    }

    public trait Sub: Super {
        override fun foo(): List<List<String>>
    }
}
