// LANGUAGE: +CompanionBlocksAndExtensions

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1
}

companion val Foo.sta<caret>tic1: Int get() = 1
