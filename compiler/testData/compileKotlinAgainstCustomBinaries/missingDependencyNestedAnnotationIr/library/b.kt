package b

import a.A

@A.Anno("B")
interface B {
    @A.Anno("foo")
    fun <@A.Anno("T") T> foo(t: T) = t
}
