open class Base<P>() {
    fun f() = 1
}
    
open class Left<P>() : Base<P>()

trait Right<P> : <!TRAIT_WITH_SUPERCLASS!>Base<P><!>

class Diamond<P>() : Left<P>(), Right<P>
