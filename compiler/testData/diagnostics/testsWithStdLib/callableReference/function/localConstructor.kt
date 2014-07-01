import kotlin.reflect.KFunction0

fun main() {
    class A
    
    val x = ::A
    x : KFunction0<A>
}
