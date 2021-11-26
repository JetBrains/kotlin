// WITH_STDLIB
// IGNORE_BACKEND: WASM

val a = "INT " + 0x8fffffffU
val b = "BYTE " + 0x8ffU
val c = "LONG " + 0xffff_ffff_ffffU

val uint = 0x8fffffffU
val ubyte = 0x8ffU
val ulong = 0xffff_ffff_ffffU

val aa = "INT " + uint
val bb = "BYTE " + ubyte
val cc = "LONG " + ulong


fun box(): String {
    if (a != "INT 2415919103") {
        return "FAIL 0: $a"
    }
    if (aa != "INT 2415919103") {
        return "FAIL 1: $aa"
    }

    if (b != "BYTE 2303") {
        return "FAIL 2: $b"
    }
    if (bb != "BYTE 2303") {
        return "FAIL 3: $bb"
    }


    if (c != "LONG 281474976710655") {
        return "FAIL 4: $c"
    }
    if (cc != "LONG 281474976710655") {
        return "FAIL 5: $cc"
    }

    return "OK"
}
