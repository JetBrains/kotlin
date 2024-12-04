// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@file:JvmName("SomeName")

@JvmField
val c = 4

@JvmField
var g = 5

class C {
    @JvmField
    var g = 5
}