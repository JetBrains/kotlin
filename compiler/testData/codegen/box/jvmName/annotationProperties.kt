// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.test.assertEquals

annotation class Anno(@get:JvmName("uglyJvmName") val value: String)

@Anno(value = "OK")
class Foo


annotation class Meta(val anno: Anno)

@Meta(Anno(value = "OK"))
fun bar() {}


fun box(): String {
    val f = Foo::class.annotations.single()
    assertEquals("@Anno(uglyJvmName=OK)", f.toString())
    assertEquals("OK", (f as Anno).value)

    val b = ::bar.annotations.single()
    assertEquals("@Meta(anno=@Anno(uglyJvmName=OK))", b.toString())
    assertEquals("OK", (b as Meta).anno.value)

    return "OK"
}
