// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
package one

class C {
    companion {
        fun greet(): String = "Hi"
        val title: String = "C"
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: C.class[getTitle;greet;title]

// DECLARATIONS_NO_LIGHT_ELEMENTS: C.class[greet;title]
