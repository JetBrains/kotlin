// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.test.assertEquals
import kotlin.test.assertTrue

annotation class Anno(@get:JvmName("uglyJvmName") val value: String)

@Anno(value = "OK")
class Foo


annotation class Meta(val anno: Anno)

@Meta(Anno(value = "OK"))
fun bar() {}

fun box(): String {
    val f = Foo::class.annotations.single()
    assertTrue("@test.Anno\\(uglyJvmName=\"?OK\"?\\)".toRegex().matches(f.toString()))
    assertEquals("OK", (f as Anno).value)

    val b = ::bar.annotations.single()
    assertEquals("@test.Meta(anno=$f)", b.toString())
    assertEquals("OK", (b as Meta).anno.value)

    return "OK"
}
