// Derived

interface Base {
    val boo: String
}

class Derived(x: Base): Base by x