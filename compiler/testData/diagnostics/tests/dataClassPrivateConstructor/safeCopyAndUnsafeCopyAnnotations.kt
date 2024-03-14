// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
@file:OptIn(ExperimentalStdlibApi::class)

@kotlin.SafeCopy
@kotlin.UnsafeCopy
data class Data(val x: Int)

@kotlin.SafeCopy
class Foo

@kotlin.UnsafeCopy
class Bar
