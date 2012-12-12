// "Specify type explicitly" "true"
// ERROR: Public or protected member should have specified type

package a

class A() {
    public val <caret>a = b.foo()
}