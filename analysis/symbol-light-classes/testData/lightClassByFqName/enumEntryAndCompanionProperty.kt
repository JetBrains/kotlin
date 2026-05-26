// fields.Foo
// WITH_STDLIB
package fields

enum class Foo {
    entry;

    companion object {
        val entry: Int = 1
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[entry;entry$1;getEntries;valueOf;values]
