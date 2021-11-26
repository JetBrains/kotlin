// JVM_TARGET: 1.8
// !JVM_DEFAULT_MODE: enable
// WITH_STDLIB
// TARGET_BACKEND: JVM
interface Base {
    fun value(): String
}

interface SubA : Base

interface SubB : Base {
    @JvmDefault
    override fun value(): String = "OK"
}

interface SubAB : SubA, SubB

fun box(): String {
    return object : SubAB {}.value()
}
