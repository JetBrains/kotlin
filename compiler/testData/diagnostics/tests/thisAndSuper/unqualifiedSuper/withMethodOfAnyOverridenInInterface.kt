interface IWithToString {
    override fun toString(): String
}

class A : IWithToString {
    // Should be Any#toString(), even though IWithToString defines an abstract toString.
    override fun toString(): String = super.toString()
}

interface IWithImplementedToString {
    override fun toString(): String = "Heh"
}

class B : IWithImplementedToString {
    override fun toString(): String = super.toString()
}
