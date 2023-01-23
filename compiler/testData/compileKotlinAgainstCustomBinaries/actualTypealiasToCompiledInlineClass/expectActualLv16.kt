package test

@kotlin.jvm.JvmInline
expect value class ExpectValueActualInline(val value: Int)

@kotlin.jvm.JvmInline
expect value class ExpectValueActualValue(val value: Int)

actual typealias ExpectValueActualInline = lib.InlineClass
actual typealias ExpectValueActualValue = lib.ValueClass
