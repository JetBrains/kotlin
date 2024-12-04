// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Base {
    var v : Int
        get() = 1
        set(v) {}
}

open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right
