package org.jetbrains.jet.samples.vfs.utils;

import java.util.concurrent.locks.Lock
import java.util.List
import kotlin.util.*

/**
 * Executes task with given lock acquired.
 */
private fun locked<T>(lock : Lock, body : () -> T) : T {
    lock.lock()
    try {
        return body()
    } finally {
        lock.unlock();
    }
}

/**
 * Returns list containing all elements which first contains and second does not.
 */
public fun listDifference<T>(first : List<T>, second : List<T>) : List<T> {
    return first.filter{ !second.contains(it) }.toList()
}

/**
 * Returns list containing all elements which both lists contain.
 */
public fun listIntersection<T>(first : List<T>, second : List<T>) : List<T> {
    return first.filter{ second.contains(it) }.toList()
}
