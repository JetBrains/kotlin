// FULL_JDK
// WITH_RUNTIME
// IGNORE_BACKEND: JS, NATIVE

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val evalStateLock = ReentrantReadWriteLock()
private val classLoaderLock = ReentrantReadWriteLock()
val compiledClasses = arrayListOf("")

fun box(): String = evalStateLock.write {
    classLoaderLock.read {
        classLoaderLock.write {
            "write"
        }

        compiledClasses.forEach {
            it
        }
    }

    classLoaderLock.read {
        compiledClasses.map { it }
    }

    "OK"
}
