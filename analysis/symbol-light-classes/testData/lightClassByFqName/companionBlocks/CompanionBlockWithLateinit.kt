// one.C
// LANGUAGE: +CompanionBlocksAndExtensions
package one

class C {
    companion {
        lateinit var name: String
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: C.class[getName;name;setName]
