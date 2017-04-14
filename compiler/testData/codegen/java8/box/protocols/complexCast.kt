// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Master {
    fun <T> id(x: T): T
}

protocol interface Slave {
    fun <A> id(x: A): A
}

fun box(): String {
    val master: Master = object : Master {
        override fun <T> id(x: T): T = x
    }
    val slave: Slave = master

    return master.id(slave.id("OK"))
}
