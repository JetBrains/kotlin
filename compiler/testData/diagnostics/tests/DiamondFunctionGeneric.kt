interface Base<P> {
    fun f() = 1
}
    
open class Left<P>() : Base<P>

interface Right<P> : Base<P>

class Diamond<P>() : Left<P>(), Right<P>
