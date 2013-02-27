class Item(val name: String, val rating: Int): Comparable<Item> {
    public override fun compareTo(other: Item): Int {
        return compareBy(this, other, { rating }, { name })
    }
}

// from standard library
inline fun <T : Any> compareBy(<!UNUSED_PARAMETER!>a<!>: T?, <!UNUSED_PARAMETER!>b<!>: T?,
                               vararg <!UNUSED_PARAMETER!>functions<!>: T.() -> Comparable<*>?): Int = throw Exception()