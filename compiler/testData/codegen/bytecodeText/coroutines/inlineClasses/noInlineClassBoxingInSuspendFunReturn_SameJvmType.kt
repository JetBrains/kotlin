interface I

inline class ICI(val i: I): I

class Wrapper(val s: String): I

suspend fun suspendICI(): ICI = ICI(Wrapper(""))
suspend fun suspendI(): I = ICI(Wrapper(""))
suspend fun <T> suspendGeneric(x: T): T = x

fun useICString(x: ICI) {}
fun useI(x: I) {}

suspend fun test() {
    useICString(suspendICI())
    useICString(suspendGeneric(ICI(Wrapper(""))))
    useI(suspendI())
    useI(suspendICI())
}

// -- 1 in 'suspendAny(): I = ICI("")'
// -- 1 in 'useI(suspendICI())'
// -- 1 in 'suspendGeneric(ICI(""))'
// 3 INVOKESTATIC ICI\.box-impl

// -- 1 in 'useICI(suspendGeneric(ICI("")))
// -- 1 in 'equals-impl' for ICI
// -- 2 in resume path of suspendICI
// 4 INVOKEVIRTUAL ICI\.unbox-impl