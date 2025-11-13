// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82311


interface I {
    fun getVal(): Int
}

@JvmInline
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

@JvmInline
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
