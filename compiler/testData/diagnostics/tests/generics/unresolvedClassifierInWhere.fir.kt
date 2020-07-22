interface I0<T : <!OTHER_ERROR!>Unresolved0<String><!>>
interface I1<T> where T : <!OTHER_ERROR!>Unresolved1<String><!>
interface I2<T : <!OTHER_ERROR!>Unresolved2<String><!>> where T : <!OTHER_ERROR!>Unresolved3<String><!>

fun <E : <!OTHER_ERROR!>Unresolved4<String><!>> foo0() {}
fun <E> foo1() where E : <!OTHER_ERROR!>Unresolved5<String><!> {}
fun <E : <!OTHER_ERROR!>Unresolved6<String><!>> foo2() where E : <!OTHER_ERROR!>Unresolved7<String><!> {}

val <E : <!OTHER_ERROR!>Unresolved7<!>> E.p1: Int
        get() = 1
val <E> E.p2: Int where E : <!OTHER_ERROR!>Unresolved8<!>
        get() = 1
val <E : <!OTHER_ERROR!>Unresolved9<!>> E.p3: Int where E : <!OTHER_ERROR!>Unresolved10<!>
        get() = 1
