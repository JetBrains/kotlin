// WITH_STDLIB
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization

// MODULE: lib
// FILE: scalars.kt
inline fun <T : Any> ifTrue(
    condition: Boolean,
    exec: () -> T?,
): T? {
    return if (condition)
        exec()
    else
        null
}

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
        println("IF line")
        bar(true) {
            println("B line")
            "OK"
        }
        return block()
    }
    return "fail"
}

// FILE: bar.kt
inline fun bar(flag: Boolean, block: () -> String): String {
    ifTrue<Unit>(flag) {
        println("BAR line")
        return block()
    }
    return "fail"
}

// FILE: test.kt
fun box(): String {
    return InlineFunCallSite().render()
}

// EXPECTATIONS NATIVE
// test.kt:51 box
// InlineFunCallSite.kt:18 <init>
// InlineFunCallSite.kt:25 <init>
// test.kt:51 box
// InlineFunCallSite.kt:19 render
// InlineFunCallSite.kt:20 render
// InlineFunDeclaration.kt:29 render
// scalars.kt:10 render
// scalars.kt:11 render
// InlineFunDeclaration.kt:30 render
// InlineFunDeclaration.kt:30 render
// InlineFunDeclaration.kt:31 render
// bar.kt:42 render
// scalars.kt:10 render
// scalars.kt:11 render
// bar.kt:43 render
// bar.kt:44 render
// InlineFunDeclaration.kt:32 render
// InlineFunDeclaration.kt:32 render
// InlineFunDeclaration.kt:33 render
// bar.kt:44 render
// InlineFunDeclaration.kt:35 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:22 render
// InlineFunDeclaration.kt:35 render
// InlineFunCallSite.kt:23 render
// InlineFunCallSite.kt:24 render
// test.kt:51 box
// test.kt:52 box

// EXPECTATIONS JS_IR
// test.kt:51 box
// InlineFunCallSite.kt:18 <init>
// test.kt:51 box
// InlineFunDeclaration.kt:30 render
// bar.kt:43 render
// InlineFunDeclaration.kt:32 render
// InlineFunCallSite.kt:21 render
// InlineFunDeclaration.kt:35 render
// InlineFunCallSite.kt:20 render

// EXPECTATIONS WASM_JS
// test.kt:51 $box (11)
// InlineFunCallSite.kt:25 $InlineFunCallSite.<init> (1)
// test.kt:51 $box (31)
// InlineFunCallSite.kt:20 $InlineFunCallSite.render (19, 15)
// InlineFunDeclaration.kt:29 $InlineFunCallSite.render (17, 4)
// scalars.kt:10 $InlineFunCallSite.render (15)
// scalars.kt:11 $InlineFunCallSite.render (8)
// InlineFunDeclaration.kt:30 $InlineFunCallSite.render (16, 8)
// InlineFunDeclaration.kt:31 $InlineFunCallSite.render (12, 8)
// bar.kt:42 $InlineFunCallSite.render (17, 4)
// scalars.kt:10 $InlineFunCallSite.render (15)
// scalars.kt:11 $InlineFunCallSite.render (8)
// bar.kt:43 $InlineFunCallSite.render (16, 8)
// bar.kt:44 $InlineFunCallSite.render (15)
// InlineFunDeclaration.kt:32 $InlineFunCallSite.render (20, 12)
// InlineFunDeclaration.kt:33 $InlineFunCallSite.render (12, 16)
// bar.kt:44 $InlineFunCallSite.render (8)
// InlineFunDeclaration.kt:35 $InlineFunCallSite.render (15)
// InlineFunCallSite.kt:21 $InlineFunCallSite.render (20, 12)
// InlineFunCallSite.kt:22 $InlineFunCallSite.render (23, 12)
// InlineFunDeclaration.kt:35 $InlineFunCallSite.render (8)
// InlineFunCallSite.kt:20 $InlineFunCallSite.render (8)
// test.kt:51 $box (4)
