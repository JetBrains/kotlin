//KT-4749

fun box(): String {
    if (0xF0: Byte != (-16).toByte()) return "fail 1"
    if (0xFF01: Short != (-255).toShort()) return "fail 2"
    if (0x90CFEA35: Int != -1865422283) return "fail 3"
    if (0xABCDEFABCDEFABCD: Long != -6066929601824707635) return "fail 4"

    if (0b11100001: Byte != (-31).toByte()) return "fail 5"
    if (0b1110000111100001: Short != (-7711).toShort()) return "fail 6"
    if (0b11100001111000011110000111100001: Int != -505290271) return "fail 7"
    if (0b1110000111100001111000011110000111100001111000011110000111100001: Long != -2170205185142300191) return "fail 8"

    return "OK"
}
