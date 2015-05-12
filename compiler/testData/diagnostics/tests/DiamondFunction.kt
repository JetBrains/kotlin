interface Base {
    fun f() = 1
}
    
open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right
