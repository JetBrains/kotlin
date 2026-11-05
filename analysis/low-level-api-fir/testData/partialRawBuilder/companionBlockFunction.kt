// FUNCTION: foo
// LANGUAGE: +CompanionBlocks +CompanionExtensions

package test.classes

class Owner {
    companion {
        fun foo(): Int {
            return bar()
        }

        fun bar(): Int = 1
    }
}
