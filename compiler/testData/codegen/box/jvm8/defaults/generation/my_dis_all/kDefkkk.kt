// CHECK_BYTECODE_TEXT

// MODULE: lib
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt

interface Base {
    fun foo(): Int = 2
}

open class Left : Base

// 1 INVOKESTATIC Base\$DefaultImpls\.foo\-pVg5ArA \(LBase;\)I



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


// 1 INVOKESPECIAL Right\.foo\-pVg5ArA \(\)I \(itf\)
// 1 INVOKESPECIAL Left\.\<init\> \(\)V

// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated