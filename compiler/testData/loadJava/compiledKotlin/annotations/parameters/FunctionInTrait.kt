//ALLOW_AST_ACCESS
package test

annotation class Anno

trait Trait {
    fun foo([Anno] x: String) = 42
}
