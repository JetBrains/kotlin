// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

interface NumberOperations {
    operator fun Number.plus(other: Number): Number
}

object DoubleOperations : NumberOperations {
    override operator fun Number.plus(other: Number) = this.toDouble() + other.toDouble()
}

data class Matrix(val rows: Int, val columns: Int, val data: Array<out Number>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Matrix

        if (rows != other.rows) return false
        if (columns != other.columns) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + data.contentHashCode()
        return result
    }
}

fun matrixOf(rows: Int, columns: Int, vararg data: Number): Matrix {
    assert(rows * columns == data.size) { "Wrong dimentions" }
    return Matrix(rows, columns, data)
}

context(NumberOperations) operator fun Matrix.plus(other: Matrix): Matrix {
    assert(rows == other.rows && columns == other.columns) { "Matrices should have the same dimentions" }
    return matrixOf(rows, columns, *data.mapIndexed { i, element -> element + other.data[i] }.toTypedArray())
}

fun box(): String {
    val m1 = matrixOf(2, 2, 1, 2, 3, 4)
    val m2 = matrixOf(2, 2, .4, .3, .2, .1)
    with(DoubleOperations) {
        return if (m1 + m2 == matrixOf(2, 2, 1.4, 2.3, 3.2, 4.1)) "OK" else "fail"
    }
}