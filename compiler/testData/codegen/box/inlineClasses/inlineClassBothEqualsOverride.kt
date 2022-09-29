// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR


interface I {
    fun getVal(): Int
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val x: Int) : I {
    override fun getVal(): Int {
        return x
    }

    fun equals(other: IC1): Boolean {
        return x == other.x
    }

    override fun equals(other: Any?): Boolean {
        if (other !is I) {
            return false
        }
        return getVal() == other.getVal()
    }

    override fun hashCode(): Int {
        return getVal()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val y: Int) : I {
    override fun getVal(): Int {
        return y * 10
    }

    fun equals(other: IC2): Boolean {
        return y == other.y
    }

    override fun equals(other: Any?): Boolean {
        if (other !is I) {
            return false
        }
        return getVal() == other.getVal()
    }

    override fun hashCode(): Int {
        return getVal()
    }
}

fun box(): String = if (setOf(IC1(10), IC2(1)).size == 1) "OK" else "Fail"