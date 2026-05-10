// LANGUAGE: +CompanionBlocksAndExtensions
class Foo {
    companion {
        val static<caret>Property: Int = 1
    }

    fun regularFunction() {

    }

    val regularProperty = 1
}
