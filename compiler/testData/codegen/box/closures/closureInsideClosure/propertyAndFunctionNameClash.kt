// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// on JS item local variable clashes with ListTag.item() extension function

package d

fun box(): String {
    ListTag().test(listOf("a", "b"))
    return "OK"
}

fun ListTag.test(list: List<String>) {
    for (item in list) {
        item() {
            a {
                text = item
            }
        }
    }
}

open class HtmlTag
open class ListTag : HtmlTag() {}
class LI : ListTag() {}

public fun ListTag.item(body: LI.() -> Unit): Unit {}
fun HtmlTag.a(contents: A.() -> Unit) {}

abstract class A : HtmlTag() {
    public abstract var text: String
}

fun listOf(vararg strings: String): List<String> {
    val list = ArrayList<String>()
    for (s in strings) {
        list.add(s)
    }
    return list
}
