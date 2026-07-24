// one.MyEnumClass
// WITH_STDLIB

package one

enum class MyEnumClass {
    Entry;

    fun foo(): Int = 0
}

// LIGHT_ELEMENTS_NO_DECLARATION: MyEnumClass.class[getEntries;valueOf;values]
