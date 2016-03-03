// B.kt
// See also KT-8344: INVOKESPECIAL instead of INVOKEVIRTUAL in accessor

package second

import first.A

public abstract class B(): A() {
    val value = {
        test()
    }
}

class C: B() {
    override fun test() = "OK"
}

