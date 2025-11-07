// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

// Android doesn't have @Repeatable before API level 24, so findAnnotations can't unpack repeatable annotations.
// IGNORE_BACKEND: ANDROID

import kotlin.test.assertEquals
import kotlin.reflect.full.findAnnotations

@JvmRepeatable(ArrayOfInt.ContainerOfInt::class)
annotation class ArrayOfInt(val ints: IntArray = []) {
    annotation class ContainerOfInt(val value: Array<ArrayOfInt>)
}

@JvmRepeatable(ArrayOfString.ContainerOfString::class)
annotation class ArrayOfString(val strings: Array<String> = []) {
    annotation class ContainerOfString(val value: Array<ArrayOfString>)
}

@JvmRepeatable(ArrayOfEnum.ContainerOfEnum::class)
annotation class ArrayOfEnum(val enums: Array<DeprecationLevel> = []) {
    annotation class ContainerOfEnum(val value: Array<ArrayOfEnum>)
}

@JvmRepeatable(ArrayOfAnnotation.ContainerOfAnnotation::class)
annotation class ArrayOfAnnotation(val annotations: Array<ArrayOfString> = []) {
    annotation class ContainerOfAnnotation(val value: Array<ArrayOfAnnotation>)
}

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
    assertEquals("[1, 2]", C::arrayOfInt.findAnnotations<ArrayOfInt>().map { it.ints.single() }.toString())
    assertEquals("[a, b]", C::arrayOfString.findAnnotations<ArrayOfString>().map { it.strings.single() }.toString())
    assertEquals("[WARNING, ERROR]", C::arrayOfEnum.findAnnotations<ArrayOfEnum>().map { it.enums.single() }.toString())
    assertEquals(
        "[a, b]",
        C::arrayOfAnnotation.findAnnotations<ArrayOfAnnotation>().map {
            it.annotations.single().strings.single()
        }.toString()
    )

    return "OK"
}
