// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM

// FILE: nestedTypeAliasSimple.kt
external interface I {
    val x: Int
}

external interface HostI {
    typealias TI = I
}

external class HostC {
    companion object
    typealias TA = I
}

external class IntPair(x: Int, y: Int) {
    val x: Int
    val y: Int
}
typealias IntPairAlias = IntPair
typealias IntPairAliasChain = IntPairAlias

@JsModule("./nestedTypeAliasSimple.mjs")
external object Holder {

    typealias TA = I

    fun consume(i: TA): Int
    fun makeInner(): TA

    fun consumeViaHostI(i: HostI.TI): Int
    fun consumeViaHostC(i: HostC.TA): Int

    fun makeIntPair(x: Int, y: Int): IntPair
    fun sumIntPair(p: IntPairAlias): Int
    fun sumIntPairChain(p: IntPairAliasChain): Int
}

fun box(): String {
    val inner = Holder.makeInner()
    val res = Holder.consume(inner)

    val res2 = Holder.consumeViaHostI(inner)
    val res3 = Holder.consumeViaHostC(inner)

    val pair = Holder.makeIntPair(10, 13)
    val sum1 = Holder.sumIntPair(pair)
    val sum2 = Holder.sumIntPairChain(pair)

    return if (
        res == 123 &&
        res2 == 123 &&
        res3 == 123 &&
        sum1 == 23 &&
        sum2 == 23
    ) "OK" else "FAIL"
}

// FILE: nestedTypeAliasSimple.mjs

export function makeInner() {
    return { x: 123 };
}

export function consume(i) {
    return i.x | 0;
}

export function consumeViaHostI(i) {
    return i.x | 0;
}

export function consumeViaHostC(i) {
    return i.x | 0;
}

export class IntPair {
    constructor(x, y) {
        this.x = x | 0;
        this.y = y | 0;
    }
}

export function makeIntPair(x, y) {
    return new IntPair(x, y);
}

export function sumIntPair(p) {
    return (p.x | 0) + (p.y | 0);
}

export function sumIntPairChain(p) {
    return (p.x | 0) + (p.y | 0);
}