// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

import kotlin.test.assertEquals

@Repeatable
annotation class ArrayOfInt(val ints: IntArray = [])

@Repeatable
annotation class ArrayOfString(val strings: Array<String> = [])

@Repeatable
annotation class ArrayOfEnum(val enums: Array<DeprecationLevel> = [])

@Repeatable
annotation class ArrayOfAnnotation(val annotations: Array<ArrayOfString> = [])

class C {
    @ArrayOfInt([1])
    @ArrayOfInt([2])
    val arrayOfInt = ""

    @ArrayOfString(["a"])
    @ArrayOfString(["b"])
    val arrayOfString = ""

    @ArrayOfEnum([DeprecationLevel.WARNING])
    @ArrayOfEnum([DeprecationLevel.ERROR])
    val arrayOfEnum = ""

    @ArrayOfAnnotation([ArrayOfString(arrayOf("a"))])
    @ArrayOfAnnotation([ArrayOfString(arrayOf("b"))])
    val arrayOfAnnotation = ""
}

fun box(): String {
    assertEquals("[1, 2]", C::arrayOfInt.annotations.filterIsInstance<ArrayOfInt>().map { it.ints.single() }.toString())
    assertEquals("[a, b]", C::arrayOfString.annotations.filterIsInstance<ArrayOfString>().map { it.strings.single() }.toString())
    assertEquals("[WARNING, ERROR]", C::arrayOfEnum.annotations.filterIsInstance<ArrayOfEnum>().map { it.enums.single() }.toString())
    assertEquals(
        "[a, b]",
        C::arrayOfAnnotation.annotations.filterIsInstance<ArrayOfAnnotation>().map {
            it.annotations.single().strings.single()
        }.toString()
    )

    return "OK"
}
