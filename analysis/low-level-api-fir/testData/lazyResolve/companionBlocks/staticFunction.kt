// LANGUAGE: +CompanionBlocksAndExtensions
class Foo {
    companion {
        fun staticFun<caret>ction() {}
    }

    fun regularFunction() {

    }

    val regularProperty = 1
}
