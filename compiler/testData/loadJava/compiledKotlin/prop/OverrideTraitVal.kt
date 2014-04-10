//ALLOW_AST_ACCESS
package test

trait Trait {
    val shape: String
}

open class Subclass() : Trait {
    override open val shape = "circle"
}
