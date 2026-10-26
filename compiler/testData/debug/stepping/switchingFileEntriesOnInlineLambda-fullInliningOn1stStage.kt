// WITH_STDLIB
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization

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
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:22 render
// bar.kt:42 render
// scalars.kt:10 render
// scalars.kt:11 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:22 render
// InlineFunCallSite.kt:23 render
// InlineFunCallSite.kt:23 render
// InlineFunCallSite.kt:27 render
// InlineFunCallSite.kt:22 render
// InlineFunCallSite.kt:27 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:22 render
// InlineFunCallSite.kt:27 render
// InlineFunCallSite.kt:23 render
// InlineFunCallSite.kt:24 render
// test.kt:51 box
// test.kt:52 box

// EXPECTATIONS JS_IR
// test.kt:51 box
// InlineFunCallSite.kt:18 <init>
// test.kt:51 box
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:21 render
// InlineFunCallSite.kt:23 render
// InlineFunCallSite.kt:21 render
// InlineFunDeclaration.kt:27 render
// InlineFunCallSite.kt:20 render

// EXPECTATIONS WASM
// test.kt:51 $box (11)
// InlineFunCallSite.kt:25 $InlineFunCallSite.<init> (1)
// test.kt:51 $box (31)
// InlineFunCallSite.kt:20 $InlineFunCallSite.render (19, 15)
// InlineFunDeclaration.kt:29 $InlineFunCallSite.render (17, 4)
// scalars.kt:10 $InlineFunCallSite.render (15)
// scalars.kt:11 $InlineFunCallSite.render (8)
// InlineFunCallSite.kt:21 $InlineFunCallSite.render (15, 7)
// InlineFunCallSite.kt:22 $InlineFunCallSite.render (8, 4)
// bar.kt:42 $InlineFunCallSite.render (17, 4)
// scalars.kt:10 $InlineFunCallSite.render (15)
// scalars.kt:11 $InlineFunCallSite.render (8)
// InlineFunCallSite.kt:21 $InlineFunCallSite.render (11, 3)
// InlineFunCallSite.kt:22 $InlineFunCallSite.render (8)
// InlineFunCallSite.kt:23 $InlineFunCallSite.render (8, 0)
// InlineFunCallSite.kt:27 $InlineFunCallSite.render (11, 15)
// InlineFunCallSite.kt:22 $InlineFunCallSite.render (1)
// InlineFunCallSite.kt:27 $InlineFunCallSite.render (41)
// InlineFunCallSite.kt:21 $InlineFunCallSite.render (20, 12)
// InlineFunCallSite.kt:22 $InlineFunCallSite.render (23, 12)
// InlineFunCallSite.kt:27 $InlineFunCallSite.render (34)
// InlineFunCallSite.kt:20 $InlineFunCallSite.render (8)
// test.kt:51 $box (4)
