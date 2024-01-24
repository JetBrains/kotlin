// IGNORE_TREE_ACCESS: KT-65268
object A {
    constructor()
    init {}
}

enum class B {
    X() {
        constructor()
    }
}

class C {
    companion object {
        constructor()
    }
}

val anonObject = object {
    constructor()
}