trait Base {
    var v : Int
        get() = 1
        set(v) {}
}
    
open class Left() : Base

trait Right : Base

class Diamond() : Left(), Right
