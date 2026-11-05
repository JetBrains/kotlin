// LANGUAGE: +CompanionBlocks +CompanionExtensions

class A {
    companion {
        val x: Int = doSmth(<expr>"str"</expr>)
    }
}

fun doSmth(i: String) = 4
