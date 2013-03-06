package test

public trait InheritReadOnlinessSameClass: Object {

    public trait Super: Object {
        public fun foo(): List<String>
    }

    public trait Sub: Super {
        override fun foo(): List<String>
    }
}
