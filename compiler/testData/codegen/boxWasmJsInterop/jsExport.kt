// TARGET_BACKEND: WASM
// MODULE: main
// FILE: externals.kt

class C(val x: Int)

@JsExport
fun makeC(x: Int): JsReference<C> = C(x).toJsReference()

@JsExport
fun getX(c: JsReference<C>): Int = c.get().x

@JsExport
fun getString(s: String): String = "Test string $s";

@JsExport
fun isEven(x: Int): Boolean = x % 2 == 0

external interface EI

@JsExport
fun eiAsAny(ei: EI): JsReference<Any> = ei.toJsReference()

@JsExport
fun anyAsEI(any: JsReference<Any>): EI = any.get() as EI

@JsExport
fun provideUByte(): UByte = UByte.MAX_VALUE

@JsExport
fun provideNullableUByte(nullable: Boolean): UByte? = if (nullable) null else UByte.MAX_VALUE

@JsExport
fun consumeUByte(x: UByte) = x.toString()

@JsExport
fun consumeNullableUByte(x: UByte?) = x?.toString()

@JsExport
fun provideUShort(): UShort = UShort.MAX_VALUE

@JsExport
fun provideNullableUShort(nullable: Boolean): UShort? = if (nullable) null else UShort.MAX_VALUE

@JsExport
fun consumeUShort(x: UShort) = x.toString()

@JsExport
fun consumeNullableUShort(x: UShort?) = x?.toString()

@JsExport
fun provideUInt(): UInt = UInt.MAX_VALUE

@JsExport
fun provideNullableUInt(nullable: Boolean): UInt? = if (nullable) null else UInt.MAX_VALUE

@JsExport
fun consumeUInt(x: UInt) = x.toString()

@JsExport
fun consumeNullableUInt(x: UInt?) = x?.toString()

@JsExport
fun provideULong(): ULong = ULong.MAX_VALUE

@JsExport
fun provideNullableULong(nullable: Boolean): ULong? = if (nullable) null else ULong.MAX_VALUE

@JsExport
fun consumeULong(x: ULong) = x.toString()

@JsExport
fun consumeNullableULong(x: ULong?) = x?.toString()

fun box(): String = "OK"

// FILE: entry.mjs

import {
    makeC,
    getX,
    getString,
    isEven,
    anyAsEI,
    eiAsAny,
    provideUByte,
    provideUShort,
    provideUInt,
    provideULong,
    provideNullableUByte,
    provideNullableUShort,
    provideNullableUInt,
    provideNullableULong,
    consumeUByte,
    consumeNullableUByte,
    consumeUShort,
    consumeNullableUShort,
    consumeUInt,
    consumeNullableUInt,
    consumeULong,
    consumeNullableULong,
} from "./index.mjs"

const c = makeC(300);
if (getX(c) !== 300) {
    throw "Fail 1";
}

if (getString("2") !== "Test string 2") {
    throw "Fail 2";
}

if (isEven(31) !== false || isEven(10) !== true) {
    throw "Fail 3";
}

if (anyAsEI(eiAsAny({x:10})).x !== 10) {
    throw "Fail 4";
}

if (provideUByte() != 255) {
    throw "Fail 5";
}
if (provideUShort() != 65535) {
    throw "Fail 6";
}
if (provideUInt() != 4294967295) {
    throw "Fail 7";
}
if (provideULong() != 18446744073709551615n) {
    throw "Fail 8";
}

if (provideNullableUByte(false) != 255) {
    throw "Fail 9";
}
if (provideNullableUByte(true) != null) {
    throw "Fail 10";
}
if (provideNullableUShort(false) != 65535) {
    throw "Fail 11";
}
if (provideNullableUShort(true) != null) {
    throw "Fail 12";
}
if (provideNullableUInt(false) != 4294967295) {
    throw "Fail 13";
}
if (provideNullableUInt(true) != null) {
    throw "Fail 14";
}
if (provideNullableULong(false) != 18446744073709551615n) {
    throw "Fail 15";
}
if (provideNullableULong(true) != null) {
    throw "Fail 16";
}

if (consumeUByte(-1) != "255") {
    throw "Fail 17";
}
if (consumeNullableUByte(-1) != "255") {
    throw "Fail 18";
}
if (consumeNullableUByte(null) != null) {
    throw "Fail 19";
}

if (consumeUShort(-1) != "65535") {
    throw "Fail 20";
}
if (consumeNullableUShort(-1) != "65535") {
    throw "Fail 21";
}
if (consumeNullableUShort(null) != null) {
    throw "Fail 22";
}

if (consumeUInt(-1) != "4294967295") {
    throw "Fail 23";
}
if (consumeNullableUInt(-1) != "4294967295") {
    throw "Fail 24";
}
if (consumeNullableUInt(null) != null) {
    throw "Fail 25";
}

if (consumeULong(-1n) != "18446744073709551615") {
    throw "Fail 26";
}
if (consumeNullableULong(-1n) != "18446744073709551615") {
    throw "Fail 27";
}
if (consumeNullableULong(null) != null) {
    throw "Fail 28";
}

if (consumeUByte(255) != "255") {
    throw "Fail 29";
}
if (consumeNullableUByte(255) != "255") {
    throw "Fail 30";
}

if (consumeUShort(65535) != "65535") {
    throw "Fail 31";
}
if (consumeNullableUShort(65535) != "65535") {
    throw "Fail 32";
}

if (consumeUInt(4294967295) != "4294967295") {
    throw "Fail 33";
}
if (consumeNullableUInt(4294967295) != "4294967295") {
    throw "Fail 34";
}

if (consumeULong(18446744073709551615n) != "18446744073709551615") {
    throw "Fail 35";
}
if (consumeNullableULong(18446744073709551615n) != "18446744073709551615") {
    throw "Fail 36";
}

if (consumeUByte(256) != "0") {
    throw "Fail 37";
}
if (consumeNullableUByte(256) != "0") {
    throw "Fail 38";
}

if (consumeUShort(65536) != "0") {
    throw "Fail 39";
}
if (consumeNullableUShort(65536) != "0") {
    throw "Fail 40";
}

if (consumeUInt(4294967296) != "0") {
    throw "Fail 41";
}
if (consumeNullableUInt(4294967296) != "0") {
    throw "Fail 42";
}

if (consumeULong(18446744073709551616n) != "0") {
    throw "Fail 43";
}
if (consumeNullableULong(18446744073709551616n) != "0") {
    throw "Fail 44";
}
