package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import java.util.concurrent.ConcurrentHashMap

typealias ConcurrentHashSet<T> = ConcurrentHashMap<T, Boolean>

fun <T> ConcurrentHashSet<T>.add(t: T) = this.put(t, true)
fun <T> ConcurrentHashSet<T>.asList() = this.keys().toList()

