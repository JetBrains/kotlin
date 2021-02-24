// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
// FIR_COMPARISON

class <caret>C {
    init {
        println("global")
    }
}

fun main(args: Array<String>) {
    C()
    class C {
        init {
            println("local")
        }
    }
    C()
}