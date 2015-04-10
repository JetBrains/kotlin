open class Base() {
    fun f() = 1
}
    
open class Left() : Base()

trait Right : <!TRAIT_WITH_SUPERCLASS!>Base<!>

class Diamond() : Left(), Right
