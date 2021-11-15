// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, infix-function-call -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: infix calls for properties
 */

class C() {
    var isInvokeCalled = false
    infix operator fun invoke(i: Int) { isInvokeCalled = true } //(1)
}

class B() {
    val memberValC
        get() = c
}
val c = C()

val B.extensionValC: C
    get() = c


fun box(): String{
    val b = B()
    b memberValC 3         //resolved to (1)
    if (b.memberValC.isInvokeCalled) {
        c.isInvokeCalled = false
        b extensionValC 4      //resolved to (1)
        if (b.extensionValC.isInvokeCalled)
            return "OK"
    }
    return "NOK"
}
