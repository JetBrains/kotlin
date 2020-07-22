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

// new contracts syntax for simple functions

fun test9(s: MyClass?) contract [returns() implies (s != null), someContract(s), returns() implies (s is MySubClass)] {
    test_9()
}

fun test10 contract [returnsNotNull()] {
    test10()
}

// new contracts syntax for property accessors

class MyClass {
    var myInt: Int = 0
        get() contract [returnsNotNull()] = 1
        set(value) {
            field = value * 10
        }
}

class AnotherClass(multiplier: Int) {
    var anotherInt: Int = 0
        get() contract [returnsNotNull()] = 1
        set(value) contract [returns()] {
            field = value * multiplier
        }
}

class SomeClass(multiplier: Int?) {
    var someInt: Int = 0
        get() contract [returnsNotNull()] = 1
        set(value) contract [returns() implies (value != null)] {
            value ?: throw NullArgumentException()
            field = value
        }
}