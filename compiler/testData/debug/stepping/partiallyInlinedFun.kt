// WITH_STDLIB
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization

// MODULE: lib
// FILE: scalars.kt
inline fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T? = if (condition) exec() else null

// MODULE: main(lib)
// FILE: InlineFunCallSite.kt
class InlineFunCallSite {
    fun render(): String {
        return foo(true) {
            println("A line")
            return@foo "OK"
        }
    }
}

// FILE: InlineFunDeclaration.kt
inline fun foo(flag: Boolean, block: () -> String): String {
    ifTrue<Unit>(flag) {
        println("A line")
        return block()
    }
    return "fail"
}

// FILE: test.kt
fun box(): String {
    return InlineFunCallSite().render()
}

// EXPECTATIONS JVM_IR
// test.kt:30 box
// InlineFunCallSite.kt:10 <init>
// test.kt:30 box
// InlineFunCallSite.kt:12 render
// InlineFunDeclaration.kt:21 render
// scalars.kt:6 render
// InlineFunDeclaration.kt:22 render
// InlineFunDeclaration.kt:23 render
// InlineFunCallSite.kt:13 render
// InlineFunCallSite.kt:14 render
// InlineFunDeclaration.kt:23 render
// InlineFunCallSite.kt:12 render
// test.kt:30 box

// EXPECTATIONS JS_IR
// test.kt:30 box
// InlineFunCallSite.kt:10 <init>
// test.kt:30 box
// scalars.kt:5 render
// InlineFunCallSite.kt:13 render
// scalars.kt:6 render
// InlineFunCallSite.kt:12 render

// EXPECTATIONS WASM
// test.kt:30 $box (11)
// InlineFunCallSite.kt:17 $InlineFunCallSite.<init> (1)
// test.kt:30 $box (31)
// InlineFunCallSite.kt:12 $InlineFunCallSite.render (19, 15)
// InlineFunDeclaration.kt:21 $InlineFunCallSite.render (17, 4)
// scalars.kt:6 $InlineFunCallSite.render (74, 85)
// InlineFunDeclaration.kt:22 $InlineFunCallSite.render (16, 8)
// InlineFunDeclaration.kt:23 $InlineFunCallSite.render (15)
// InlineFunCallSite.kt:13 $InlineFunCallSite.render (20, 12)
// InlineFunCallSite.kt:14 $InlineFunCallSite.render (23, 12)
// InlineFunDeclaration.kt:23 $InlineFunCallSite.render (8)
// InlineFunCallSite.kt:12 $InlineFunCallSite.render (8)
// test.kt:30 $box (4)
