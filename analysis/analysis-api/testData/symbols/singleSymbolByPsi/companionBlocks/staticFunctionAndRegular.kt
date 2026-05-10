// LANGUAGE: +CompanionBlocksAndExtensions
class Foo {
    fun function(): Int = 1

    companion {
        fun funct<caret>ion(): Int = 2
    }
}
