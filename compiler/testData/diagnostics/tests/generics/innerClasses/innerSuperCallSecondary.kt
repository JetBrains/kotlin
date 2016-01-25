// !DIAGNOSTICS: -UNUSED_PARAMETER
open class Super<T> {
    inner open class Inner {
    }
}

class Sub : Super<String>() {
    inner class SubInner : Super<String>.Inner {
        constructor()
        constructor(x: Int) : super() {}
    }
}
