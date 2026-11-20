// fields.Foo
package fields

enum class Foo {
    entry;

    companion object {
        val entry: Int by lazy { 1 }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[entry;entry$delegate;entry_delegate$lambda$0;getEntries;valueOf;values]
