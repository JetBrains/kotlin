// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: overload-resolution, receivers -> paragraph 6 -> sentence 5
 * PRIMARY LINKS: overload-resolution, receivers -> paragraph 6 -> sentence 4
 * NUMBER: 1
 * DESCRIPTION: Superclass companion object receivers are prioritized according to the inheritance order
 */


fun box(): String {
    val case0 =CaseBase0().case0()
    val case1 =CaseBase1().case1()
    val case2 =CaseBase2().case2()
    if (case0 && case1 && case2)
        return "OK"
    return "NOK"
}

class CaseBase2() : CaseBase1() {

    companion object foo {
        var isCaseCompanionCalled = false
        fun foo() {
            isCaseCompanionCalled = true
        }
    }

    fun case2(): Boolean {
        foo.foo()
        val res = !isCaseBase0ReceiverCalled && !isCaseBaseReceiverCalled && isCaseCompanionCalled
        isCaseCompanionCalled = false
        return res
    }
}

open class CaseBase1 : CaseBase0() {
    companion object foo {
        var isCaseBaseReceiverCalled = false
        fun foo() {
            this.isCaseBaseReceiverCalled = true
        }
    }

    fun case1(): Boolean {
        foo.foo()
        val res = !isCaseBase0ReceiverCalled && CaseBase1.isCaseBaseReceiverCalled && !CaseBase2.isCaseCompanionCalled
        isCaseBaseReceiverCalled = false
        return res
    }

}

open class CaseBase0 {
    companion object foo {
        var isCaseBase0ReceiverCalled = false
        fun foo() {
            this.isCaseBase0ReceiverCalled = true
        }
    }

    fun case0(): Boolean {
        foo.foo()
        val res = isCaseBase0ReceiverCalled && !CaseBase1.isCaseBaseReceiverCalled && !CaseBase2.isCaseCompanionCalled
        isCaseBase0ReceiverCalled = false
        return res
    }
}
