inline class ICInt(val x: Int) // unbox-impl in generated 'equals'

suspend fun suspendICInt(): ICInt = ICInt(1) // box-impl
suspend fun suspendAny(): Any = ICInt(1) // box-impl
suspend fun <T> suspendGeneric(x: T): T = x

fun useICInt(x: ICInt) {}
fun useAny(x: Any) {}

suspend fun test() {
    useICInt(suspendICInt()) // unbox-impl
    useICInt(suspendGeneric(ICInt(1))) // box-impl, unbox-impl
    useAny(suspendAny())
    useAny(suspendICInt())
}

// 3 INVOKESTATIC ICInt\.box-impl
// 3 INVOKEVIRTUAL ICInt\.unbox-impl