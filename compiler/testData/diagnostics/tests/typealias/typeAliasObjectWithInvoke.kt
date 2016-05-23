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
val test5 = CWI(<!TOO_MANY_ARGUMENTS!>null<!>)
val test5a = ClassWithCompanionObjectWithInvoke(<!TOO_MANY_ARGUMENTS!>null<!>)
