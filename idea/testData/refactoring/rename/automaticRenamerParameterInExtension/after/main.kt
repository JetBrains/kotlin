package testing

interface Trait {
    open fun Int.foo(aa: Int, b: String) {
    }
}

open class Super {
    open fun Int.foo(aa: Int, b: String) {
    }
}

open class Middle : Super(), Trait {
    override fun Int.foo(/*rename*/aa: Int, b: String) {
    }
}

class Sub : Middle() {
    override fun Int.foo(aa: Int, b: String) {
    }
}
