// TARGET_BACKEND: JVM

interface A {
    override operator fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

class AImpl : A {
    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

interface B
class BImpl : B

class Impl : A by AImpl(), B by BImpl()

fun box(): String {
    Impl()
    return "OK"
}
