// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class CustomGetVal() {
    val zz = 1
        get() = field * 2
}
