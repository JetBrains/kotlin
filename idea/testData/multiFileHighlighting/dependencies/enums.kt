package enums

interface Base {
    fun f() {
    }
}

enum class E(val i: Int = 0): Base {
    E1: E() {
        override fun f() {
        }
    }
    E2: E(3) {
        override fun f() {
        }
    }
    E3
}