// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
package one

class C {
    companion {
        @Deprecated("use newApi instead", ReplaceWith("newApi()"))
        fun oldApi(): Int = 0

        fun newApi(): Int = 1

        @Deprecated("use newProperty instead")
        val oldProperty: String = "old"
    }
}
