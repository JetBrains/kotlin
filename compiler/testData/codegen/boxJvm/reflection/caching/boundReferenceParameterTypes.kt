// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.KClassifier

data class D(
    val p01: Int,
    val p02: Int? = 0,
    val p03: Array<Int> = arrayOf(),
    val p04: Array<Int>? = null,
    val p05: String = "",
    val p06: String? = null,
)

private fun getParameterTypeClassifiers(): List<KClassifier> =
    D(1)::copy.parameters.map { it.type.classifier!! }

fun box(): String {
    val a = getParameterTypeClassifiers()
    val b = getParameterTypeClassifiers()
    for ((x, y) in a.zip(b)) {
        if (x !== y) return "Fail: classifier seems to be not cached"
    }
    return "OK"
}
