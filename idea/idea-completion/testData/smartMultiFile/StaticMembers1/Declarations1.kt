package dependency1

import test.C

object O1 {
    fun foo(): C = C()
    fun bar(): C = C()
    fun x(): String = ""
}