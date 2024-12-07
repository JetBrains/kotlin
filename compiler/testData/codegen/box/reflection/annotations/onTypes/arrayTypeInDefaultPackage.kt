// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

annotation class Nested(val value: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno(
    val aa: Array<Nested>,
)

fun f(): @Anno([Nested("OK")]) Unit {}

fun box(): String {
    val anno = ::f.returnType.annotations.single() as Anno
    return anno.aa[0].value
}
