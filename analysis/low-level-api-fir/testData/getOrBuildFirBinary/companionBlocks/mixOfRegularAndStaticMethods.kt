// LANGUAGE: +CompanionBlocksAndExtensions
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
class Foo {
    fun regular1() {

    }

    companion {
        val static1: Int = 1
    }

    val regular2: String = "r2"
    fun regular3() {}

    companion {
        val static2: Long = 2L
        fun static3() {}
    }

    fun regular4() {}
}
