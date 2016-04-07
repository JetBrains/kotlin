package testing

interface Trait {
    open fun foo(aa: Int, b: String) {
    }
}

open class Super {
    open fun foo(aa: Int, b: String) {
    }
}

open class Middle : Super(), Trait {
    override fun foo(aa: Int, b: String) {
    }
}

class Sub : Middle() {
    override fun foo(aa: Int, b: String) {
    }
}
