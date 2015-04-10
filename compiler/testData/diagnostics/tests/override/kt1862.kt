open class Aaa() {
    open fun foo() = 1
}

open class Bbb() : Aaa() {
    override fun foo() = 2
}

trait Ccc : <!TRAIT_WITH_SUPERCLASS!>Aaa<!>

class Ddd() : Bbb(), Ccc