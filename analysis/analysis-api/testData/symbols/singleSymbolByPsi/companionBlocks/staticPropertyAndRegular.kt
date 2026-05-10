// LANGUAGE: +CompanionBlocksAndExtensions
class Foo {
    val property = "regular"

    companion {
        val propert<caret>y = "static"
    }
}
