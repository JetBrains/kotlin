// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
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

// LIGHT_ELEMENTS_NO_DECLARATION: C.class[first;firstName;getFirstName;getSecondName;second;secondName]

// DECLARATIONS_NO_LIGHT_ELEMENTS: C.class[first;firstName;second;secondName]
