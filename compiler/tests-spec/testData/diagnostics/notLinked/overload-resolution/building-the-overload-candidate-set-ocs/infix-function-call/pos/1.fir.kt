// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// SKIP_TXT

class C() {
    infix operator fun invoke(i: Int) { } //(1)
}

class B(val memberValCNull: C? = null) {
    infix fun bar(i: Int) {}
}

// TESTCASE NUMBER: 1
fun case1() {
    val b: B = B()
//    b memberValCNull 1                 //nok  (UNSAFE_INFIX_CALL)
//    b.memberValCNull.invoke(1)         //nok (UNSAFE_CALL)

    if (b.memberValCNull != null) {
        b memberValCNull 1            //nok  (UNSAFE_INFIX_CALL) !!!!
        b.memberValCNull.invoke(2)    //ok
        b.memberValCNull(3)           //nok (UNSAFE_CALL)  !!!
    }
}
