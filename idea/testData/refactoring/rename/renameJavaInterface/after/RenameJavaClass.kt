package kotlin.testing

import testing.NewInterfaceName

class Some(s: NewInterfaceName) : NewInterfaceName() {
    val test = s

    fun testFun(param : NewInterfaceName) : NewInterfaceName {
        return test;
    }
}