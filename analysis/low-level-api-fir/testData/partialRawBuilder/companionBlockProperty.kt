// PROPERTY: foo
// LANGUAGE: +CompanionBlocks +CompanionExtensions

package test.classes

class Owner {
    companion {
        val foo: Int
            get() {
                return bar()
            }

        fun bar(): Int = 1
    }
}
