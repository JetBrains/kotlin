// one.MyEnumClass
// LANGUAGE: +EnumEntries
// WITH_STDLIB

package one

enum class MyEnumClass {
    Entry;

    fun doo(): Int = 0
}

// LIGHT_ELEMENTS_NO_DECLARATION: MyEnumClass.class[getEntries;valueOf;values]
