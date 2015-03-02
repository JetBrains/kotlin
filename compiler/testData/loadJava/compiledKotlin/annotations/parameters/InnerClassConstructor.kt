//ALLOW_AST_ACCESS
package test

annotation class A(val s: String)

class Outer {
    class Nested([A("nested")] val x: String)

    inner class Inner([A("inner")] val y: String)
}
