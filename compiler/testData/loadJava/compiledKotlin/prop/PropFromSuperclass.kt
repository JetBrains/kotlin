//ALLOW_AST_ACCESS
package test

open class BaseClass() {
    val exactly = { 17 }()
}

class Subclass() : BaseClass() {
}
