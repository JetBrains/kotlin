// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Foo {
    companion {
        val staticDelegated<caret>Property: Int by lazy { 1 }
    }
}
