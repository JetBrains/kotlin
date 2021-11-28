// FIR_IGNORE
// Should have raw description
fun test_1() {
//  [ERROR: not resolved]
//  │
    contract {
//      [ERROR: not resolved]
//      │
        callsInPlace()
    }
//  fun test_1(): Unit
//  │
    test_1()
}

fun test_2() {
//                   fun contracts/contract(contracts/ContractBuilder.() -> Unit): Unit
//                   │        contract@0
//                   │        │
    kotlin.contracts.contract {
//      fun <R> (contracts/ContractBuilder).callsInPlace<???>(Function<R>, contracts/InvocationKind = ...): contracts/CallsInPlace
//      this@0
//      │
        callsInPlace()
//      fun <R> (contracts/ContractBuilder).callsInPlace<???>(Function<R>, contracts/InvocationKind = ...): contracts/CallsInPlace
//      this@0
//      │
        callsInPlace()
    }
//  fun test_2(): Unit
//  │
    test_2()
}

//  Int           Int
//  │             │
var test_3: Int = 1
    get() {
//      [ERROR: not resolved]
//      │
        contract {
//          [ERROR: not resolved]
//          │
            callsInPlace()
        }
//             Int
//             │
        return 1
    }
//      Int
//      │
    set(value) {
//                       fun contracts/contract(contracts/ContractBuilder.() -> Unit): Unit
//                       │        contract@0
//                       │        │
        kotlin.contracts.contract {
//          fun <R> (contracts/ContractBuilder).callsInPlace<???>(Function<R>, contracts/InvocationKind = ...): contracts/CallsInPlace
//          this@0
//          │
            callsInPlace()
//          fun <R> (contracts/ContractBuilder).callsInPlace<???>(Function<R>, contracts/InvocationKind = ...): contracts/CallsInPlace
//          this@0
//          │
            callsInPlace()
        }
    }

fun test_4() {
//  [ERROR: not resolved]
//  │
    contract()
//  fun test_4(): Unit
//  │
    test_4()
}

// should not have raw description

fun test_5() {
//  fun test_5(): Unit
//  │
    test_5()
//  [ERROR: not resolved]
//  │
    contract()
}

fun test_6() {
//  [ERROR: not resolved]
//  │   [ERROR: not resolved]
//  │   │   [ERROR: not resolved]
//  │   │   │   [ERROR: not resolved]
//  │   │   │   │
    aaa.bbb.ccc.contract {

    }
//  fun test_6(): Unit
//  │
    test_6()
}

fun test_7() {
//  [ERROR: not resolved]
//  │         [ERROR: not resolved]
//  │         │
    contracts.contract {

    }
//  fun test_7(): Unit
//  │
    test_7()
}

fun test_8() {
//  [ERROR: not resolved]
//  │   [ERROR: not resolved]
//  │   │      [ERROR: not resolved]
//  │   │      │         [ERROR: not resolved]
//  │   │      │         │
    aaa.kotlin.contracts.contract {

    }
//  fun test_8(): Unit
//  │
    test_8()
}
