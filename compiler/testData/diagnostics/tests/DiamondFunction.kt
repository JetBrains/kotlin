// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Base {
    fun f() = 1
}
    
open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right
