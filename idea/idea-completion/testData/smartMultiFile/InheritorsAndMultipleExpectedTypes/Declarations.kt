class X
class Y

trait T1
trait T2<T>
trait T3<T>

class C1 : T1
class C2<T> : T2<T>, T3<T>
class C3 : T1, T2<X>

fun foo(p: T1){}
fun foo(p: T2<X>){}
fun foo(p: T3<Y>){}

// ALLOW_AST_ACCESS
