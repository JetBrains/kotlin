// see org.jetbrains.jet.plugin.highlighter.HtmlTabledDescriptorRendererTest

import java.io.Closeable

class MyList<T>(t: T) {}

fun <T> getMyList(t: T): MyList<T> = MyList(t)
fun <T, E: Cloneable> writeToMyList (l: MyList< in T>, t: T) where E: Closeable {}
class Cons<T, E: Cloneable>(l: MyList<in T>, t: T)

fun test1(int: Int, any: Any) {
    writeToMyList(getMyList(int), any)
    Cons(getMyList(int), any)
}
