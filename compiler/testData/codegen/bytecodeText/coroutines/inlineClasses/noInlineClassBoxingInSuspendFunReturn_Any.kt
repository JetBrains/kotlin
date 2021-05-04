inline class ICAny(val x: Any)

suspend fun suspendICAny(): ICAny = ICAny("")
suspend fun suspendAny(): Any = ICAny("")
suspend fun <T> suspendGeneric(x: T): T = x

fun useICAny(x: ICAny) {}
fun useAny(x: Any) {}

suspend fun test() {
    useICAny(suspendICAny())
    useICAny(suspendGeneric(ICAny("")))
    useAny(suspendAny())
    useAny(suspendICAny())
}

// -- 1 in 'suspendAny(): Any = ICAny("")'
// -- 1 in 'useAny(suspendICAny())'
// -- 1 in 'suspendGeneric(ICAny(""))'
// 3 INVOKESTATIC ICAny\.box-impl

// -- 1 in 'useICAny(suspendGeneric(ICAny("")))
// -- 1 in 'equals-impl' for ICAny
// -- 2 on resume path of suspendICAny
// 4 INVOKEVIRTUAL ICAny\.unbox-impl