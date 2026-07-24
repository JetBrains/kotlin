// LANGUAGE: +IntrinsicConstEvaluation +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
fun <T> T.id() = this

class A {
    companion object {
        const val x = (1 + 2).toByte()
        const val y = 1.inc().toByte()
    }
}

class B {
    companion {
        const val x = (1 + 2).toByte()
        const val y = 1.inc().toByte()
    }
}

class C
companion const val C.x = (1 + 2).toByte()
companion const val C.y = 1.inc().toByte()

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (A.x.id() != 3.toByte()) return "Fail A.x"
    if (A.y.id() != 2.toByte()) return "Fail A.y"

    if (B.x.id() != 3.toByte()) return "Fail B.x"
    if (B.y.id() != 2.toByte()) return "Fail B.y"

    if (C.x.id() != 3.toByte()) return "Fail C.x"
    if (C.y.id() != 2.toByte()) return "Fail C.y"

    return "OK"
}
