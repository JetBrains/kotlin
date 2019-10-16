package localClass

fun main(args: Array<String>) {
    class LocalClass {
        fun test() = 1
    }

    val local = LocalClass()
    //Breakpoint!
    val a = 1
}

// EXPRESSION: local
// RESULT: instance of localClass.LocalClassKt$main$LocalClass(id=ID): LlocalClass/LocalClassKt$main$LocalClass;

// EXPRESSION: local.test()
// RESULT: 1: I