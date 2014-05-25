// Derived

trait Base {
    fun baz(s: String): String
}

class Derived(x: Base): Base by x