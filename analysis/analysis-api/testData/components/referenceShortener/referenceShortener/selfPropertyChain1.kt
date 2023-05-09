// FILE: main.kt
package foo

object Foo {
    val self = this

    fun selfFun(): Foo = this

    <expr>
    fun check() {
        self.self
        selfFun().self
    }
    </expr>
}