// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Raise() {
    var zz = 1
        set(it) { field = it / 2 }
}
