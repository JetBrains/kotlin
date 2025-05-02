// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: topLevelTypeAliasSimple.kt
external interface I { val x: Int }

typealias TA = I
typealias TB = TA
typealias TNullable = TA?
typealias ToStr = (TA) -> String

@JsModule("./topLevelTypeAliasSimple.mjs")
external object Holder {
    fun make(): TA
    fun makeB(): TB
    fun consume(i: TA): Int
    fun consumeTB(i: TB): Int
    fun consumeNullable(i: TNullable): Int
    fun makeToStr(): ToStr
}

fun box(): String {
    val a = Holder.make()
    val b = Holder.makeB()

    val r1 = Holder.consume(a)
    val r2 = Holder.consumeTB(b)
    val r3 = Holder.consumeNullable(null)
    val r4 = Holder.makeToStr()(a)

    return if (r1 == 123 && r2 == 321 && r3 == -1 && r4 == "x=123") "OK" else "FAIL"
}

// FILE: topLevelTypeAliasSimple.mjs
export function make() {
    return { x: 123 };
}

export function makeB() {
    return { x: 321 };
}

export function consume(i) {
    return i.x | 0;
}

export function consumeTB(i) {
    return i.x | 0;
}

export function consumeNullable(i) {
    return i ? (i.x | 0) : -1;
}


export function makeToStr() {
    return (i) => "x=" + i.x;
}