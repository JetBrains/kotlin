// Derived

interface Base {
    fun baz(g: String): String
}

class Derived(x: Base): Base by x