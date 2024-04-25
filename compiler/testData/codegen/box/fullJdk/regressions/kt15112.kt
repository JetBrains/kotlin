// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM

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
