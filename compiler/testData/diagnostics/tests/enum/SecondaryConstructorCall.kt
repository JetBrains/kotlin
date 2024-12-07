// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// KT-7753 false positive: enum constructor can be called from secondary constructor
enum class A(val c: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    FORTY_TWO();

    constructor(): this(42)   
}