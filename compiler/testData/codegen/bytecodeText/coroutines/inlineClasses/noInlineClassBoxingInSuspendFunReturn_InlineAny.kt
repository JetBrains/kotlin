inline class IC0(val x: Any) // IC0.unbox-impl in generated 'equals'
inline class IC1(val x: IC0) // IC1.unbox-impl in generated 'equals'

suspend fun suspendIC(): IC1 = IC1(IC0(""))
suspend fun suspendAny(): Any = IC1(IC0("")) // IC1.box-impl
suspend fun <T> suspendGeneric(x: T): T = x

fun useIC(x: IC1) {}
fun useAny(x: Any) {}

suspend fun test() {
    useIC(suspendIC()) // IC1.unbox-impl of resume path
    useIC(suspendGeneric(IC1(IC0("")))) // IC1.box-impl, IC1.unbox-impl
    useAny(suspendAny())
    useAny(suspendIC()) // IC1.box-impl, IC1.unbox-impl of resume path
}

// 0 INVOKESTATIC IC0\.box-impl
// 3 INVOKESTATIC IC1\.box-impl

// 1 INVOKEVIRTUAL IC0\.unbox-impl
// 4 INVOKEVIRTUAL IC1\.unbox-impl
