
import java.util.Collection;
import java.util.List;

open class A

class B : A()

fun ff(l: Collection<B>) = l is List<out A>

