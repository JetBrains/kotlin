// LANGUAGE: +CompanionBlocks +CompanionExtensions

class Owner<T>(val constructorProperty: Int) {
    val memberProperty: Int = 1

    companion {
        val companionProperty: Int = 2

        fun foo(): Int {
            return <expr>companionProperty</expr>
        }
    }

    companion object {
        val companionObjectProperty: Int = 3
    }
}
