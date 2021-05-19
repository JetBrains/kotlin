import kotlin.*
import kotlin.ranges.*

@CompileTimeCalculation
class MatrixNN(val values: Array<Array<Double>>) {
    val size = values.size
    operator fun times(other: MatrixNN): MatrixNN {
        val matrix = Array<Array<Double>>(size) { Array<Double>(size) { 0.0 } }
        for (i in 0 until size) {
            for (j in 0 until size) {
                for (k in 0 until size) {
                    matrix[i][j] += this.values[i][k] * other.values[k][j]
                }
            }
        }
        return MatrixNN(matrix)
    }
}

@CompileTimeCalculation
fun demo(): Double {
    val m1 = MatrixNN(
        arrayOf(
            arrayOf(3.0, 1.0, 0.0),
            arrayOf(1.0, 1.0, 0.0),
            arrayOf(0.0, 0.0, 1.0)
        )
    )
    val m2 = MatrixNN(
        arrayOf(
            arrayOf(3.0, 1.0, 1.0),
            arrayOf(1.0, 1.0, 1.0),
            arrayOf(1.0, 1.0, 1.0)
        )
    )

    return (m1 * m2).values[0][0]
}

const val temp = <!EVALUATED: `10.0`!>demo()<!>
