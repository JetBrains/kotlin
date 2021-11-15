// Tests that are inspired by the stack-related and verifier-related bugs in the wasm backend

// WITH_STDLIB

fun box(): String {
    if (!test1()) return "Fail 1"
    if (!test2()) return "Fail 2"
    if (!test3()) return "Fail 3"
    if (!test4()) return "Fail 4"
    if (!test5()) return "Fail 5"
    if (!test6()) return "Fail 6"
    if (!test7()) return "Fail 7"
    if (!test8()) return "Fail 8"
    return "OK"
}

open class Foo: Throwable("") {}
class Bar: Foo() {}
class Baz: Foo() {}
class Darb: Throwable("") {}

fun ooo() {
    throw Baz()
}

fun zoot(): String {
    return "str"
}

// Standard exception handling case without finally
fun test1(): Boolean {
    try {
        ooo()
    } catch (b: Bar) {
        throw Darb()
        return false
    } catch (b: Baz) {
        return true
    } catch (b: Darb) {
        return false
    }

    return false
}

// Standart case with finally
fun test2(): Boolean {
    var catched = false

    try {
        ooo()
    } catch (b: Bar) {
        throw Darb()
        return false
    } catch (b: Baz) {
        catched = true
        return false
    } catch (b: Darb) {
        return false
    } finally {
        return catched
    }

    return false
}


// Fallthrough with value on the stack (only needs to compile)
fun test3(): Boolean {
    try {
        1
    } catch (e: Throwable) {
        2
    }

    return true
}

// Fallthrough with value on the stack and finally
fun test4(): Boolean {
    var seenFinally = false

    try {
        ooo()
        2
    } catch (b: Throwable) {
        1
    } finally {
        seenFinally = true
    }

    return seenFinally
}

// Try with return value which is used later
fun test5(): Boolean {
    val arg = try {
        ooo()
        1
    } catch (b: Baz) {
        3
    } catch (b: Darb) {
        4
    }

    return arg == 3
}


// Case where catch uses labeled return which doesn't end the catch
fun foo_for_test6(): String {
    var ret = ""

    try {
        ooo()
    } catch (e: Throwable) {
        listOf(1, 2, 3, 4, 5).forEach {
            if (it == 3) return@forEach
        }
        ret += "O"
    } finally {
        ret += "K"
    }

    return ret
}

fun test6(): Boolean {
    return foo_for_test6() == "OK"
}

// Catch is ended with the loop break into outer loop
fun test7(): Boolean {
    var num_exc = 0
    var num_breaks = 0
    var num_finallies = 0
    var num_bodies = 0

    loop@ for (i in 1..3) {
        for (j in 1..5) {
            try {
                ooo()
            } catch (e: Throwable) {
                ++num_exc
                if (i == 2 || i == 4) {
                    ++num_breaks
                    break@loop
                }
            } finally {
                ++num_finallies
            }
            ++num_bodies
        }
    }

    if (num_exc == 6 && num_breaks == 1 && num_finallies == 6 && num_bodies == 5)
        return true
    return false
}

// Finally throws an exception
class Baobab: Throwable()
class Zanzibar: Throwable()
class Hypo(val catchedBaobab: Boolean, val thrownZanzibar: Boolean, val seenFinally: Boolean): Throwable()

fun golb() {
    throw Baobab()
}

fun foo(i: Int) {
    var catchedBaobab = false
    var thrownZanzibar = false
    var seenFinally = false

    try {
        golb()
    } catch (b: Baobab) {
        catchedBaobab = true
        if (i == 9) {
            thrownZanzibar = true
            throw Zanzibar()
        }
    } finally {
        seenFinally = true
        throw Hypo(catchedBaobab, thrownZanzibar, seenFinally)
    }
}

fun test8(): Boolean {
    try {
        foo(9)
    } catch (z: Hypo) {
        if (z.catchedBaobab && z.seenFinally && z.thrownZanzibar)
            return true
        return false
    } catch (e: Throwable) {
        return false
    }
    return false
}
