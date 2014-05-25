// Derived

trait Base {
    val boo: String
}

class Derived(x: Base): Base by x