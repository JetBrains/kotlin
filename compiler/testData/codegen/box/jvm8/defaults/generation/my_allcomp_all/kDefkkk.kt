// CHECK_BYTECODE_TEXT

// MODULE: lib
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib.kt

interface Base {
    fun foo(): Int = 2
}

open class Left : Base





// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// FILE: main.kt

interface Right : Base {
    override fun foo(): Int = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
//    println(a.foo())
    return if(a.foo() == 3) { "OK" } else { "Fail" }
}


// 0 INVOKESPECIAL Left\.\<init\> \(\)V

// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated