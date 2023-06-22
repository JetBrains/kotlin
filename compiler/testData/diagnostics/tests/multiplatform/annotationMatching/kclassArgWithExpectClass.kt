// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
import kotlin.reflect.KClass

annotation class Ann(val clazz: KClass<*>)

@Ann(LinkToExpectInnerClass.Inner::class)
expect class LinkToExpectInnerClass {
    object Inner
}

expect class WillBeTypealiased

@Ann(WillBeTypealiased::class)
expect fun linkToExpectClassWhichWillBeTypealiased()

@Ann(WillBeTypealiased::class)
expect fun linkToExpectClassWhichWillBeTypealiased2()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(LinkToExpectInnerClass.Inner::class)
actual class LinkToExpectInnerClass {
    actual object Inner
}

actual typealias WillBeTypealiased = Any

@Ann(WillBeTypealiased::class)
actual fun linkToExpectClassWhichWillBeTypealiased() {}

@Ann(Any::class)
actual fun linkToExpectClassWhichWillBeTypealiased2() {}
