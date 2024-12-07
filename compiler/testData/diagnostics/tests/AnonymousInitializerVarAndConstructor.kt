// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// http://youtrack.jetbrains.net/issue/KT-419

class A(w: Int) {
    var c = w

    init {
        c = 81
    }
}

