enum class EnumWithConstructor(val i: Int) {
    Entry(1),
    EntryWithBody(2) {
        fun foo() {
            i
        }
    }
}

enum class EnumWithoutConstructor {
    Entry,
    EntryWithBody {
        fun baz() {
            this.baz()
        }
    }
}

fun usage(with: EnumWithConstructor) {
    with.ordinal

    EnumWithConstructor.Entry
    EnumWithConstructor.Entry.name
    EnumWithConstructor.EntryWithBody
    EnumWithConstructor.EntryWithBody.i

    EnumWithoutConstructor.entries
    EnumWithoutConstructor.Entry
    EnumWithoutConstructor.EntryWithBody
}
