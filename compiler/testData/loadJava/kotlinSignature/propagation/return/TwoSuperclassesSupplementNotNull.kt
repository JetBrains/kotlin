package test

public trait TwoSuperclassesSupplementNotNull: Object {

    public trait Super1: Object {
        public fun foo(): Function0<String?>
    }

    public trait Super2: Object {
        public fun foo(): Function0<String>?
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): Function0<String>
    }
}
