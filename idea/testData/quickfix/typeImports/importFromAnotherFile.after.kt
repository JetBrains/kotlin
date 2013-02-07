// "Specify type explicitly" "true"
// ERROR: Public or protected member should have specified type

package a

import b.B

class A() {
    public val <caret>a: B = b.foo()
}