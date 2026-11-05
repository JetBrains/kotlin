// LANGUAGE: +CompanionBlocks +CompanionExtensions

class Owner<T>(val constructorProperty: Int) {
    val memberProperty: Int = 1

    companion {
        val companionProperty: Int = 2

        val foo: Int = <expr>companionProperty</expr>
    }
}
