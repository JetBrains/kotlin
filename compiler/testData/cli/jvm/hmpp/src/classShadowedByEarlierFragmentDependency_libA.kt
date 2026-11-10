package lib

// libA and libB contain the same class. Only its name matters to the failing lookup.
class Dup {
    fun ok() = "OK"
}