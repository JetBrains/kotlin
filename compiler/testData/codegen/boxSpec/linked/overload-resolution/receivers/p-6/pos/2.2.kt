// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: overload-resolution, receivers -> paragraph 6 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: implicit this receiver has higher priority than any companion object receiver
 */


class Case1() : Case1Base() {
    var isImplicitReceiverCalled = false

    fun foo() {
        this.isImplicitReceiverCalled = true
    } // (1)

    fun case() {
        foo() // resolved (1)
    }
}

open class Case1Base {

    companion object foo{
        var isCompanionObjectReceiverCalled = false
        operator fun invoke() {}
        fun foo() {
            this.isCompanionObjectReceiverCalled = true
        }
    }

}

fun box(): String {
    val test = Case1()
    test.case()
    if (test.isImplicitReceiverCalled && !Case1Base.isCompanionObjectReceiverCalled)
        return "OK"
    return "NOK"
}
