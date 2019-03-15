//ALLOW_AST_ACCESS
package test

class A {
    val a: Int = { 3 }()
    val c: Int = { 3 }()
    val Int.a: Int get() = 3
    val Int.b: Int get() = 4
    val Int.c: Int get() = 4
}