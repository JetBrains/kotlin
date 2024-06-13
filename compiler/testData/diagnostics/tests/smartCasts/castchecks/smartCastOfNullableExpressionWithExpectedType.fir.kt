// DIAGNOSTICS: -UNUSED_VARIABLE

class Item(val link: String?)

fun test(item: Item) {
    if (item.link != null) {
        val href: String = item.link
    }
}