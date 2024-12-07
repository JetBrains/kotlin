// LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

// Android doesn't have @Repeatable before API level 24.
// IGNORE_BACKEND: ANDROID

package test

import kotlin.test.assertEquals
import kotlin.reflect.full.*

@java.lang.annotation.Repeatable(As::class)
annotation class A(val value: Int)

annotation class As(val value: Array<A>)

@A(1)
@As([A(2), A(3)])
class Z

@As([A(1), A(2)])
@A(3)
class ZZ

// JDK 9+ uses {} for array arguments instead of [], JDK 15+ doesn't render "value="
fun Any?.render(): String =
    toString().replace("value=", "").replace("{", "[").replace("}", "]")

// Explicit container is not unwrapped.
fun box(): String {
    assertEquals("[@test.A(1), @test.As([@test.A(2), @test.A(3)])]", Z::class.annotations.render())
    assertEquals("[@test.A(1)]", Z::class.findAnnotations<A>().render())
    assertEquals("@test.A(1)", Z::class.findAnnotation<A>().render())

    assertEquals("[@test.As([@test.A(1), @test.A(2)]), @test.A(3)]", ZZ::class.annotations.render())
    assertEquals("[@test.A(3)]", ZZ::class.findAnnotations<A>().render())
    assertEquals("@test.A(3)", ZZ::class.findAnnotation<A>().render())

    return "OK"
}
