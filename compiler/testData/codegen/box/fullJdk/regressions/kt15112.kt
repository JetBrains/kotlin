// FULL_JDK
// WITH_STDLIB
// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63864

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
