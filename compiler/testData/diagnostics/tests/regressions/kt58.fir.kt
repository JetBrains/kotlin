//KT-58 Allow finally around definite returns

package kt58

import java.util.concurrent.locks.Lock

fun <T> lock(lock : Lock, body :  () -> T) : T {
    lock.lock()
    try {
        return body()
    }
    finally {
        lock.unlock(); // we report an error, but we chouldn't
    }
}

//more tests
fun t1() : Int {
    try {
        return 1
    }
    finally {
        return 2
    }
}

fun t2() : Int {
    try {
        return 1
    }
    finally {
        doSmth(3)
    }
}

fun t3() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        doSmth(2)
    }
    finally {
        doSmth(3)
    }
}

fun t4() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        doSmth(2)
    }
}

fun t5() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        return 2
    }
}

fun t6() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        return 2
    }
    finally {
        doSmth(3)
    }
}

fun t7() : Int {
    try {
        doSmth(1)
    }
    catch (e: UnsupportedOperationException) {
        return 2
    }
    finally {
        doSmth(3)
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun doSmth(i: Int) {
}
