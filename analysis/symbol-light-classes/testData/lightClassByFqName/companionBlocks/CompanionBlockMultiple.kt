// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
package one

class C {
    companion {
        fun first(): Int = 1
        val firstName: String = "first"
    }

    companion {
        fun second(): Int = 2
        val secondName: String = "second"
    }
}
