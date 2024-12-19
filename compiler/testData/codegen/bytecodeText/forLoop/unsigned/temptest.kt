// CHECK_BYTECODE_TEXT

// MODULE: lib
// JVM_DEFAULT_MODE: disable
// WITH_STDLIB
// FILE: lib.kt

interface Base {
    fun foo(): UInt = 2u
}

open class Left : Base

// 1 INVOKESTATIC Base\$DefaultImpls\.foo\-pVg5ArA \(LBase;\)I



// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// WITH_STDLIB
// FILE: main.kt

interface Right : Base {
    override fun foo(): UInt = 3u
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
//    println(a.foo())
    return if(a.foo() == 3u) { "OK" } else { "Fail" }
}


// 1 INVOKESPECIAL Right\.foo\-pVg5ArA \(\)I \(itf\)


// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated