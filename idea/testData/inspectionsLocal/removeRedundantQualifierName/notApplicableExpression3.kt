// PROBLEM: none
package my.simple.name

class Foo {
    val f = this
    fun check() {
        f<caret>.f.f.f.f.f.f.f.f
    }
}