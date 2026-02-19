// LANGUAGE: +KotlinFunInterfaceConstructorReference
// WITH_REFLECT
import kotlin.reflect.*

fun interface KRunnable {
    fun run()
}

typealias KR = KRunnable

fun interface KSupplier<T> {
    fun get(): T
}

typealias KSS = KSupplier<String>

fun interface KConsumer<T> {
    fun accept(x: T)
}

typealias KCS = KConsumer<String>

fun test1() = ::KRunnable

fun test1a() = ::KR

fun test1b(): KFunction<KRunnable> = ::KRunnable

fun test2(): (() -> String) -> KSupplier<String> = ::KSupplier

fun test2a(): (() -> String) -> KSupplier<String> = ::KSS

fun test3(): ((String) -> Unit) -> KConsumer<String> = ::KConsumer

fun test3a(): ((String) -> Unit) -> KConsumer<String> = ::KCS

fun test3b(): KFunction<KConsumer<String>> = ::KConsumer
