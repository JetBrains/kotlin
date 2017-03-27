package kotlinx.cinterop

internal fun decodeFromUtf8(bytes: ByteArray) = String(bytes)
internal fun encodeToUtf8(str: String) = str.toByteArray()

fun bitsToFloat(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)
fun bitsToDouble(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

// TODO: the functions below should eventually be intrinsified

inline fun <reified R : Number> Number.signExtend(): R {
    val returnValueClass = R::class.java
    when (returnValueClass) {
        java.lang.Byte::class.java -> if (this is Byte) {
            return this.toByte() as R
        }
        java.lang.Short::class.java -> if (this is Byte || this is Short) {
            return this.toShort() as R
        }
        java.lang.Integer::class.java -> if (this is Byte || this is Short || this is Int) {
            return this.toInt() as R
        }
        java.lang.Long::class.java -> if (this is Byte || this is Short || this is Int || this is Long) {
            return this.toLong() as R
        }
    }

    throw Error("unable to sign extend ${this.javaClass.simpleName} \"$this\" to ${returnValueClass.simpleName}")
}

inline fun <reified R : Number> Number.narrow(): R {
    val returnValueClass = R::class.java
    when (returnValueClass) {
        java.lang.Byte::class.java -> if (this is Byte || this is Short || this is Int || this is Long) {
            return this.toByte() as R
        }
        java.lang.Short::class.java -> if (this is Short || this is Int || this is Long) {
            return this.toShort() as R
        }
        java.lang.Integer::class.java -> if (this is Int || this is Long) {
            return this.toInt() as R
        }
        java.lang.Long::class.java -> if (this is Long) {
            return this.toLong() as R
        }
    }

    throw Error("unable to narrow ${this.javaClass.simpleName} \"$this\" to ${returnValueClass.simpleName}")
}
