// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-73256 (not supported in K1)
// LANGUAGE: +AnnotationAllUseSiteTarget +PropertyParamAnnotationDefaultTargetMode

annotation class My

@Target(AnnotationTarget.FIELD)
annotation class Your

@JvmRecord
data class Some(@all:My val x: Int, @My @field:My val y: Int, @all:Your val z: Int)

fun box(): String {
    val recordComponents = Some::class.java.recordComponents

    if (recordComponents[0].annotations.isEmpty()) {
        return "FAIL: no record component annotation for '@all:My val x' found"
    }

    if (recordComponents[1].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@My val y' found, but it should not be so"
    }

    if (recordComponents[2].annotations.isNotEmpty()) {
        return "FAIL: record component annotation for '@all:Your val z' found, but it should not be so"
    }

    return "OK"
}
