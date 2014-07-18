import java.io.Closeable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.*

// Non-local returns for these functions are temporarily disabled because they contain try-finally blocks and
// the compiler doesn't correctly include the "finally" section into the inlined result.
// Once the compiler is fixed (KT-5506), non-local returns can be re-allowed for these functions

fun testSynchronized(): Int {
    synchronized("") {
        <!RETURN_NOT_ALLOWED!>return 1<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


fun testUse(f: Closeable): Int {
    f.use {
        <!RETURN_NOT_ALLOWED!>return 2<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


fun testWithLock(l: Lock): Int {
    l.withLock {
        <!RETURN_NOT_ALLOWED!>return 3<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


fun testRead(l: ReentrantReadWriteLock): Int {
    l.read {
        <!RETURN_NOT_ALLOWED!>return 4<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


fun testWrite(l: ReentrantReadWriteLock): Int {
    l.write {
        <!RETURN_NOT_ALLOWED!>return 5<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
