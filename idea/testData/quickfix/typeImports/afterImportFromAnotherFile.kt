// "Specify type explicitly" "true"

package a

import b.B

class A() {
    public val <caret>a: B = b.foo()
}