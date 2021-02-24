// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
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
