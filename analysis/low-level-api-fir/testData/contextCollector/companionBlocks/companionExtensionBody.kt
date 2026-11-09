// LANGUAGE: +CompanionBlocks +CompanionExtensions

class Owner {
    companion {
        val companionProperty: Int = 2
    }
}

companion fun Owner.foo(): Int {
    return <expr>companionProperty</expr>
}
