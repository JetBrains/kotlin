// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
import kotlin.reflect.KClass

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val clazz: KClass<*>)

@Ann(LinkToExpectInnerClass.Inner::class)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>LinkToExpectInnerClass<!> {
    object Inner
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>WillBeTypealiased<!>

<!CONFLICTING_OVERLOADS!>@Ann(WillBeTypealiased::class)
expect fun linkToExpectClassWhichWillBeTypealiased()<!>

<!CONFLICTING_OVERLOADS!>@Ann(WillBeTypealiased::class)
expect fun linkToExpectClassWhichWillBeTypealiased2()<!>

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
