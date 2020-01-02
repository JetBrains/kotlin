open class Aaa() {
    open fun foo() = 1
}

open class Bbb() : Aaa() {
    override fun foo() = 2
}

interface Ccc : Aaa

class Ddd() : Bbb(), Ccc