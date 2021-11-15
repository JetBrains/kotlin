// TARGET_BACKEND: JVM
// WITH_STDLIB
// !OPT_IN: kotlin.experimental.ExperimentalTypeInference

import kotlin.experimental.ExperimentalTypeInference

fun <E> produce(@BuilderInference block: Derived<E>.() -> Unit): E = null as E

interface Derived<in E> : Base<E>

interface Base<in E>

interface Receiver<out E>

fun <E, C : Base<E>> Receiver<E>.toChannel(destination: C): C = null as C

fun <R> foo(r: Receiver<R>): R = produce { r.toChannel(this) }

fun box() = "OK"
