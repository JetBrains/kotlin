// IGNORE_BACKEND_FIR: JVM_IR
class CInt(val value: Int)
val nCInt3: CInt? = CInt(3)

class CLong(val value: Long)
val nCLong3: CLong? = CLong(3)

var subjectEvaluated: Int = 0

fun <T> subject(x: T): T {
    subjectEvaluated++
    return x
}

fun doTestInt(i: Int?) =
        when (subject(i)) {
            null -> "null"
            0 -> "zero"
            nCInt3?.value -> "three"
            42 -> "magic"
            else -> "other"
        }

fun doTestLong(i: Long?) =
        when (subject(i)) {
            null -> "null"
            0L -> "zero"
            nCLong3?.value -> "three"
            42L -> "magic"
            else -> "other"
        }

fun testInt(i: Int?): String {
    subjectEvaluated = 0
    val result = doTestInt(i)
    if (subjectEvaluated != 1) throw AssertionError("Subject evaluated $subjectEvaluated")
    return result
}

fun testLong(i: Long?): String {
    subjectEvaluated = 0
    val result = doTestLong(i)
    if (subjectEvaluated != 1) throw AssertionError("Subject evaluated $subjectEvaluated")
    return result
}

fun box(): String {
    return when {
        testInt(null) != "null" -> "Fail testInt null"
        testInt(0) != "zero" -> "Fail testInt 0"
        testInt(1) != "other" -> "Fail testInt 1"
        testInt(3) != "three" -> "Fail testInt 3"
        testInt(42) != "magic" -> "Fail testInt 42"

        testLong(null) != "null" -> "Fail testLong null"
        testLong(0L) != "zero" -> "Fail testLong 0"
        testLong(1L) != "other" -> "Fail testLong 1"
        testLong(3L) != "three" -> "Fail testLong 3"
        testLong(42L) != "magic" -> "Fail testLong 42"
        
        else -> "OK"
    }
}