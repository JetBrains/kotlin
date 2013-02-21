// "Specify type explicitly" "true"
// ERROR: Public or protected member should have specified type

package a

import b.B

class A() {
    public val a: B<caret> = b.foo()
}