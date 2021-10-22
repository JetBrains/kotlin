// TARGET_BACKEND: JVM
// WITH_RUNTIME
// SKIP_TXT
// KT-49339

@JvmInline
value class A(val a: Int) {
    @get:Synchronized
    val f0
        get() = Unit

    @Synchronized
    fun f1() = Unit

    @Synchronized
    fun String.f2() = Unit

    @get:Synchronized
    val String.f3
        get() = Unit

    @get:Synchronized
    val A.f4
        get() = Unit

    @Synchronized
    fun A.f5() = Unit
}

class Usual {

    @get:Synchronized
    val A.f6
        get() = Unit

    @Synchronized
    fun A.f7() = Unit

}

@Synchronized
fun A.f8() = Unit

@get:Synchronized
val A.f9
    get() = Unit

fun main() {
    val a = A(2)
    synchronized(a) {}
    synchronized(2) {}
    synchronized(0x2) {}
    synchronized(2U) {}
    synchronized(true) {}
    synchronized(2L) {}
    synchronized(2.to(1).first) {}
    synchronized(2.toByte()) {}
    synchronized(2UL) {}
    synchronized(2F) {}
    synchronized(2.0) {}
    synchronized('2') {}
    for (b in listOf(a)) {
        synchronized(b) {}
        synchronized(b.to(1).first) {}
    }
}
