// FIR_IDENTICAL
// SKIP_TXT
// ISSUE: KT-52691

fun test() = compose(C1())

class C1 : FunInterface {
    override fun invoke() = C2()()
}

class C2 : FunInterface {
    override fun invoke() {}
}

fun interface FunInterface : () -> Unit

fun compose(funInterfaces: FunInterface) = funInterfaces
