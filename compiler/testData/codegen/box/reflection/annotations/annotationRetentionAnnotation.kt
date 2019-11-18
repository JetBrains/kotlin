// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

fun box(): String {
    val a = Anno::class.annotations

    if (a.size != 1) return "Fail 1: $a"
    val ann = a.single() as? Retention ?: return "Fail 2: ${a.single()}"
    assertEquals(AnnotationRetention.RUNTIME, ann.value)

    return "OK"
}
