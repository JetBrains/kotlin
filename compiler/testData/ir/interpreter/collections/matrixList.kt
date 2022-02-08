import kotlin.*
import kotlin.ranges.*
import kotlin.collections.*

@CompileTimeCalculation
class MatrixNN(val values: List<List<Double>>) {
    val size = values.size
    operator fun times(other: MatrixNN): MatrixNN {
        val matrix = List<MutableList<Double>>(size) { MutableList<Double>(size) { 0.0 } }
        for (i in 0 until size) {
            for (j in 0 until size) {
                for (k in 0 until size) {
                    matrix[i][j] = matrix[i][j] + (this.values[i][k] * other.values[k][j])
                }
            }
        }
        return MatrixNN(matrix)
    }
}

@CompileTimeCalculation
fun demo(): Double {
    val m1 = MatrixNN(
        listOf(
            listOf(3.0, 1.0, 0.0),
            listOf(1.0, 1.0, 0.0),
            listOf(0.0, 0.0, 1.0)
        )
    )
    val m2 = MatrixNN(
        listOf(
            listOf(3.0, 1.0, 1.0),
            listOf(1.0, 1.0, 1.0),
            listOf(1.0, 1.0, 1.0)
        )
    )

    return (m1 * m2).values[0][0]
}

const val temp = <!EVALUATED: `10.0`!>demo()<!>
