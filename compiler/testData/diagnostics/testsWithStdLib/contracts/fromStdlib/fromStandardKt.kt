// !LANGUAGE: +ReadDeserializedContracts +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

fun testRunWithUnitReturn() {
    val x: Int
    run { x  = 42 }
    println(x)
}

fun testRunWithReturnValue() {
    val x: Int
    val y = run {
        x = 42
        "hello"
    }
    println(x)
    println(y)
}

fun testRunWithCoercionToUnit() {
    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>x<!>: Int
    run {
        x = 42
        "hello"
    }
}

fun testRunWithReceiver(x: Int) {
    val s: String
    x.run {
        s = this.toString()
    }
    println(s)
}

fun testWith(x: Int) {
    val s: String
    with(x) {
        s = toString()
    }
    println(s)
}

fun testApply(x: Int) {
    val y: Int
    val z: Int = x.apply { y = 42 }
    println(y)
    println(z)
}

fun testAlso(x: Int) {
    val y: Int
    x.also { y = it + 1 }
    println(y)
}

fun testLet(x: Int) {
    val z: Int
    val y: String = x.let {
        z = 42
        (it + 1).toString()
    }
    println(z)
    println(y)
}

fun testTakeIf(x: Int?) {
    val y: Int
    x.takeIf {
        y = 42
        it != null
    }
    println(y)
}

fun testTakeUnless(x: Int?) {
    val y: Int
    x.takeIf {
        y = 42
        it != null
    }
    println(y)
}

fun testRepeatOnVal(x: Int) {
    val y: Int
    repeat(x) {
        // reassignment instead of captured val initialization
        <!VAL_REASSIGNMENT!>y<!> = 42
    }
    println(<!UNINITIALIZED_VARIABLE!>y<!>)
}

fun testRepeatOnVar(x: Int) {
    var y: Int
    repeat(x) {
        // no reassignment reported
        y = 42
    }
    // but here we still unsure if 'y' was initialized
    println(<!UNINITIALIZED_VARIABLE!>y<!>)
}

fun testRepeatOnInitializedVar(x: Int) {
    var y: Int = 24
    repeat(x) {
        y = 42
    }
    println(y)
}