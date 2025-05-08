// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: JVM_IR, JS_IR
// KT-77599 K2: HMPP compilation scheme: ClassCastException on typealias actualization

// MODULE: lib-common
expect class LibClass1 { fun foo(): String }
expect class LibClass2 { fun foo(): String }
expect class LibClass3 { fun foo(): String }


// MODULE: lib-inter()()(lib-common)
expect class LibInterClass3 { fun foo(): String }

actual class LibClass1 { actual fun foo(): String = "1" }
actual typealias LibClass2 = LibClass1
actual typealias LibClass3 = LibInterClass3

// MODULE: lib-platform()()(lib-inter)
actual class LibInterClass3 { actual fun foo(): String = "2" }

// MODULE: app-common(lib-common)
//lc2: LibClass2: java.lang.ClassCastException: org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl cannot be cast to org.jetbrains.kotlin.ir.symbols.IrClassSymbol
//lc3: LibClass3: java.lang.ClassCastException: org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl cannot be cast to org.jetbrains.kotlin.ir.symbols.IrClassSymbol

fun test_common( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}

// MODULE: app-inter(lib-inter)()(app-common)
// Key /LibClass2 is missing in the map.
// Key /LibClass3 is missing in the map.
fun test_inter( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}

// MODULE: app-platform(lib-platform)()(app-inter)
//lc2: LibClass2 - org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl cannot be cast to org.jetbrains.kotlin.ir.symbols.IrClassSymbol
//lc3: LibClass3 - org.jetbrains.kotlin.ir.symbols.impl.IrTypeAliasSymbolImpl cannot be cast to org.jetbrains.kotlin.ir.symbols.IrClassSymbol
fun test_platform( lc1: LibClass1, lc2: LibClass2, lc3: LibClass3) {
    lc1.foo()
    lc2.foo()
    lc3.foo()
}
fun box(): String {
    return "OK"
}