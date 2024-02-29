class G<T>
interface Tr

fun f(q: Tr) = <!USELESS_IS_CHECK!>q is G<*><!>
