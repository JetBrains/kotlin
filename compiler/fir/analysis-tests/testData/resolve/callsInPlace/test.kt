// !DUMP_CFG
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun dummy(block: (() -> Unit)): Int {
    if (true) {
        return 0
    } else {
        return 1
    }
    block()
}

//fun dummy(block: (()->Unit)) {
//    for(i in 0 until -6){
//        block()
//    }
//}
//
//@ExperimentalContracts
//fun (()->Unit).myInvoke() {
//    contract {
//        callsInPlace(this@myInvoke, InvocationKind.EXACTLY_ONCE)
//    }
//    this()
//}
//
//@ExperimentalContracts
//fun otherInvoke(block: (()->Unit)) {
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
//    block()
//}
//
//@ExperimentalContracts
//fun ((Any) -> Unit).foo(x: Int, y: () -> Unit, z: () -> Unit){
//    if(true){
//        this(x)
//        return
//    }
//    dummy(y)
//    otherInvoke(z)
//    z.myInvoke()
//}