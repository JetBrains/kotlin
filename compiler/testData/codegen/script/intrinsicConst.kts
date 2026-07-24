// LANGUAGE: +IntrinsicConstEvaluation

class A {
    companion object {
        const val x = 1 + 2
        const val y = 1.inc()
    }
}

val result = A.x + A.y

// expected: result: 5
