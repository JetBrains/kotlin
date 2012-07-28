open class Base() {
    fun f() = 1
}
    
open class Left() : Base()

trait Right : Base

class Diamond() : Left(), Right
