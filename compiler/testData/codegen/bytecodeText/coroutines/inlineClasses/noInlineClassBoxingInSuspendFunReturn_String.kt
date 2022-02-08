inline class ICString(val x: String)

suspend fun suspendICString(): ICString = ICString("")
suspend fun suspendAny(): Any = ICString("")
suspend fun <T> suspendGeneric(x: T): T = x

fun useICString(x: ICString) {}
fun useAny(x: Any) {}

suspend fun test() {
    useICString(suspendICString())
    useICString(suspendGeneric(ICString("")))
    useAny(suspendAny())
    useAny(suspendICString())
}

// -- 1 in 'suspendAny(): Any = ICString("")'
// -- 1 in 'useAny(suspendICString())'
// -- 1 in 'suspendGeneric(ICString(""))'
// 3 INVOKESTATIC ICString\.box-impl

// -- 1 in 'useICString(suspendGeneric(ICString("")))
// -- 1 in 'equals-impl' for ICString
// -- 2 in resume path of suspendICString
// 4 INVOKEVIRTUAL ICString\.unbox-impl