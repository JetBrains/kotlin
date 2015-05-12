interface Base {
    var v : Int
        get() = 1
        set(v) {}
}
    
open class Left() : Base

interface Right : Base

class Diamond() : Left(), Right
