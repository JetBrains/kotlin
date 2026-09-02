// ISSUE: KT-89057
package pack

class <caret>B : A() {
    override fun foo(x : Int /* comment1 */ = 1 /* comment2 */, y: Int) {}
}

open class A {
    open fun foo(x: Int = 1, y: Int) {}
}
