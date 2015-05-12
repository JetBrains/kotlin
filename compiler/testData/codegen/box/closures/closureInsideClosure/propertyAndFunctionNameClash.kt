package d

import java.util.ArrayList

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

interface A : HtmlTag {
    public var text: String
}

fun listOf(vararg strings: String): List<String> {
    val list = ArrayList<String>()
    for (s in strings) {
        list.add(s)
    }
    return list
}