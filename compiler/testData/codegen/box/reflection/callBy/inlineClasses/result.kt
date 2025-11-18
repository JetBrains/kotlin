// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible

class C(val x: Result<Int>)

fun box(): String {
    val resultCtor = Result::class.constructors.single()
    val r = resultCtor.apply { isAccessible = true }.callBy(mapOf(resultCtor.parameters.single() to 42))
    if (r != Result.success(42)) return "Fail 1: $r"

    val ctorWithResult = ::C
    val s = ctorWithResult.callBy(mapOf(ctorWithResult.parameters.single() to r)).x
    if (s != Result.success(42)) return "Fail 2: $s"

    return "OK"
}
