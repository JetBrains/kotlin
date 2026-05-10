// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowExpectValueClassesWithNoPrimaryConstructor
// WORKS_WHEN_VALUE_CLASS
// WITH_STDLIB
// MODULE: common

expect value class CommonUSize : Comparable<CommonUSize> {
    override operator fun compareTo(other: CommonUSize): Int
}

fun compareUSize(a: CommonUSize, b: CommonUSize) = a.compareTo(b)

expect value class CommonSomething

// MODULE: platform()()(common)

actual typealias CommonUSize = UInt

OPTIONAL_JVM_INLINE_ANNOTATION
actual value class CommonSomething(val value: Int)

fun box() = "OK".also {
    compareUSize(20.toUInt(), 30.toUInt())
    CommonSomething(10).value
}
