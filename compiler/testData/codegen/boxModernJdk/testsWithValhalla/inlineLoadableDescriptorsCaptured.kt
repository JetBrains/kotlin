// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// MODULE: lib
// FILE: lib.kt
value class Val(val a: Int, val b: Int)
value class Other(val c: Int, val d: Int)

interface Box {
    fun get(): Any
}

inline fun makeBox(crossinline f: () -> Any): Box = object : Box {
    override fun get(): Any = f()
}

inline fun makeBoxCapturing(v: Val, crossinline f: () -> Any): Box = object : Box {
    override fun get(): Any = if (v.a > 0) f() else v
}

inline fun makeLambda(crossinline f: () -> Any): () -> Any = { f() }

inline fun makeBoxDefault(v: Val, crossinline f: () -> Any = { v }): Box = object : Box {
    override fun get(): Any = f()
}

inline fun makeBoxNested(crossinline f: () -> Any): Box = makeBox { f() }

// MODULE: main(lib)
// FILE: box.kt
// Each call site regenerates the object or lambda of the inline function with fields for the values captured by the lambda passed to
// it: `Other` here, `Integer` for an `Int?`, `Val` in the default lambda of `makeBoxDefault`, and `Holder` for `this`.
fun useBox(o: Other): Box = makeBox { o }

fun useBoxBoxedInt(i: Int?): Box = makeBox { i ?: 0 }

fun useBoxCapturing(v: Val, o: Other): Box = makeBoxCapturing(v) { o }

fun useLambda(o: Other): () -> Any = makeLambda { o }

fun useBoxDefault(v: Val): Box = makeBoxDefault(v)

fun useBoxNested(o: Other): Box = makeBoxNested { o }

value class Holder(val x: Int, val y: Int) {
    fun useBox(): Box = makeBox { this }
}

fun box(): String {
    if (useBox(Other(1, 2)).get() != Other(1, 2)) return "useBox"
    if (useBoxBoxedInt(15).get() != 15) return "useBoxBoxedInt"
    if (useBoxCapturing(Val(3, 4), Other(5, 6)).get() != Other(5, 6)) return "useBoxCapturing"
    if (useLambda(Other(7, 8))() != Other(7, 8)) return "useLambda"
    if (useBoxDefault(Val(9, 10)).get() != Val(9, 10)) return "useBoxDefault"
    if (useBoxNested(Other(11, 12)).get() != Other(11, 12)) return "useBoxNested"
    if (Holder(13, 14).useBox().get() != Holder(13, 14)) return "Holder.useBox"
    return "OK"
}

// 2 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LOther;, Ljava/lang/Integer;, LVal;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
