// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.reflect.full.createType
import kotlin.test.assertEquals

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class SourceType

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE)
annotation class BinaryType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class RuntimeType

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class SourceFunction

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class BinaryFunction

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RuntimeFunction

fun box(): String {
    val type = String::class.createType(annotations = listOf(
        SourceType(), BinaryType(), RuntimeType(), SourceFunction(), BinaryFunction(), RuntimeFunction(),
    ))

    // Annotations are not rendered in `KType.toString` right now.
    assertEquals("kotlin.String", type.toString())

    val expected =
        if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true)
            // Type annotations were ignored in the legacy implementation of `createType`.
            "[]"
        else
            // Retention and target do not matter for `createType`, all annotations should be here.
            "[@test.SourceType(), @test.BinaryType(), @test.RuntimeType(), @test.SourceFunction(), @test.BinaryFunction(), @test.RuntimeFunction()]"

    assertEquals(expected, type.annotations.toString())

    return "OK"
}
