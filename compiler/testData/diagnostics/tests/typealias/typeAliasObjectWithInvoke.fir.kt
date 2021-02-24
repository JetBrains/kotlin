// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

object ObjectWithInvoke {
    operator fun invoke() = this
}

class ClassWithCompanionObjectWithInvoke {
    companion object {
        operator fun invoke(x: Any) = x
    }
}

typealias WI = ObjectWithInvoke

typealias CWI = ClassWithCompanionObjectWithInvoke

val test1 = WI()
val test2 = <!INAPPLICABLE_CANDIDATE!>WI<!>(null)

val test3 = CWI()
val test4 = CWI("")
val test5 = <!INAPPLICABLE_CANDIDATE!>CWI<!>(null)
val test5a = <!INAPPLICABLE_CANDIDATE!>ClassWithCompanionObjectWithInvoke<!>(null)
