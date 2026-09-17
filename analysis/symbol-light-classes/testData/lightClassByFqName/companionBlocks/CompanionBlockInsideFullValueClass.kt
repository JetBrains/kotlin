// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions +FullValueClasses
// WITH_STDLIB
package one

value class C(val first: Int, val second: Int) {
    fun member(): Int = first + second

    companion {
        fun fromInt(value: Int): C = C(value, value)
    }
}
