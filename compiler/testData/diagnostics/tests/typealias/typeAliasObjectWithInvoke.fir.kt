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
val test2 = WI(<!TOO_MANY_ARGUMENTS!>null<!>)

val test3 = CWI()
val test4 = CWI("")
val test5 = CWI(<!ARGUMENT_TYPE_MISMATCH!>null<!>)
val test5a = ClassWithCompanionObjectWithInvoke(<!ARGUMENT_TYPE_MISMATCH!>null<!>)
