// WITH_STDLIB

const val a = "INT " + 0x8fffffffU
const val b = "BYTE " + 0x8ffU
const val c = "LONG " + 0xffff_ffff_ffffU

const val uint = 0x8fffffffU
const val ubyte = 0x8ffU
const val ulong = 0xffff_ffff_ffffU

const val aa = "INT " + uint
const val bb = "BYTE " + ubyte
const val cc = "LONG " + ulong


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
