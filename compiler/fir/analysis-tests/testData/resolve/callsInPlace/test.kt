// !DUMP_CFG
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

//
////fun dummy(block: (() -> Unit)): Int {
////    if (true) {
////        return 0
////    } else {
////        if(false) return 1
////    }
////    block()
////    return -2
////}
//
//fun dummy(block: (() -> Unit)) {
//    for (i in 0..0) {
//        block()
//    }
//}
//
//@ExperimentalContracts
//fun (() -> Unit).myInvoke() {
//    contract {
//        callsInPlace(this@myInvoke, InvocationKind.EXACTLY_ONCE)
//    }
//    this()
//}
//
//@ExperimentalContracts
//fun otherInvoke(block: (() -> Unit)) {
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
//    block()
//}
//
//@ExperimentalContracts
//fun ((Any) -> Unit).foo(x: Int, y: () -> Unit, z: () -> Unit) {
//    contract {
//        callsInPlace(z, InvocationKind.EXACTLY_ONCE)
//    }
//    if (true) {
//        this(x)
//    }
//    dummy(y)
//    otherInvoke(z)
////    z.myInvoke()
//}
//fun kek(block : () -> Unit){
//
//}

@ExperimentalContracts
fun (() -> Unit).dummy(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
    }
    val var1 = this

    val var2: () -> Unit
    var2 = y

    z.invoke()
}