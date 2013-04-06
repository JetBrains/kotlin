package test

public trait TwoSuperclassesConflictingProjectionKinds: Object {

    public trait Super1: Object {
        public fun foo(): MutableCollection<CharSequence>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2: Object {
        public fun foo(): MutableCollection<out CharSequence>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): MutableCollection<CharSequence>
    }
}
