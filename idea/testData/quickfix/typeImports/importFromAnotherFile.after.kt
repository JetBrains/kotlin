// "Specify type explicitly" "true"

package a

import b.B

class A() {
    public val a: B<caret> = b.foo()
}