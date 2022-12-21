// one.MyEnumClass
// !LANGUAGE: +EnumEntries
// IDEA tests don't support !LANGUAGE directive:
// COMPILER_ARGUMENTS: -XXLanguage:+EnumEntries

package one

enum class MyEnumClass {
    Entry;

    fun doo(): Int = 0
}
