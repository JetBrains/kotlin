class X
class Y

interface T1
interface T2<T>
interface T3<T>

class C1 : T1
class C2<T> : T2<T>, T3<T>
class C3 : T1, T2<X>

fun foo(p: T1){}
fun foo(p: T2<X>){}
fun foo(p: T3<Y>){}

// ALLOW_AST_ACCESS
