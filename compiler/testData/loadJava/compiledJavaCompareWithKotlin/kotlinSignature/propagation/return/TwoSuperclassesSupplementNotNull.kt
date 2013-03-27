package test

public trait TwoSuperclassesSupplementNotNull: Object {

    public trait Super1: Object {
        public fun foo(): List<String?>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2: Object {
        public fun foo(): List<String>?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): List<String>
    }
}
