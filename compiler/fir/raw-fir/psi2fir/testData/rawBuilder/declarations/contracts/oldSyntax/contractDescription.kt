// Should have raw description
fun test_1() {
    contract {
        callsInPlace()
    }
    test_1()
}

fun test_2() {
    kotlin.contracts.contract {
        callsInPlace()
        callsInPlace()
    }
    test_2()
}

var test_3: Int = 1
    get() {
        contract {
            callsInPlace()
        }
        return 1
    }
    set(value) {
        kotlin.contracts.contract {
            callsInPlace()
            callsInPlace()
        }
    }

fun test_4() {
    contract()
    test_4()
}

// should not have raw description

fun test_5() {
    test_5()
    contract()
}

fun test_6() {
    aaa.bbb.ccc.contract {

    }
    test_6()
}

fun test_7() {
    contracts.contract {

    }
    test_7()
}

fun test_8() {
    aaa.kotlin.contracts.contract {

    }
    test_8()
}