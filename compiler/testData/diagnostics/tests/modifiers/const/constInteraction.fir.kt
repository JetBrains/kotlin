const val aConst = 1
const val bConst = aConst + 1

const val boolVal = bConst > 1 || (B.boolVal && A.boolVal)
const val stringInterpolation = "Result: ${B.boolVal}"

object A {
    const val boolVal = bConst + 3 == 5

    const val recursive1: Int = 1 + B.recursive2
}

class B {
    companion object {
        const val boolVal = A.boolVal
        const val recursive2: Int = A.recursive1 + 2
    }
}
