package kotlin.testing

import testing.NewName

class Some : NewName() {
    val test = NewName()

    fun testFun(param : NewName) : NewName {
        return test;
    }
}