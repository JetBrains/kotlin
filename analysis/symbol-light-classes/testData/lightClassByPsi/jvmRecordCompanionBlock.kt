// JVM_TARGET: 17
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +CompanionBlocks

package pkg

@JvmRecord
data class MyRec(val name: String) {
    companion {
        val a0 = 0
        var a1: Int = 0
        const val a2 = 0
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: MyRec.class[a1]
