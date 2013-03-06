package test

public trait TwoSuperclassesMutableAndNot: Object {

    public trait Super1: Object {
        public fun foo(): MutableCollection<String>
    }

    public trait Super2: Object {
        public fun foo(): List<String>
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): MutableList<String>
    }
}
