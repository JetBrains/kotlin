interface I0<T : Unresolved0<String>>
interface I1<T> where T : Unresolved1<String>
interface I2<T : Unresolved2<String>> where T : Unresolved3<String>

fun <E : Unresolved4<String>> foo0() {}
fun <E> foo1() where E : Unresolved5<String> {}
fun <E : Unresolved6<String>> foo2() where E : Unresolved7<String> {}

val <E : Unresolved7> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : Unresolved8
        get() = 1
val <E : Unresolved9> E.p3: Int where E : Unresolved10
        get() = 1
