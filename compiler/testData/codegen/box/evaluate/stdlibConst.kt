// WITH_STDLIB
fun <T> T.id() = this

const val code = '1'.code

const val byteFloorDiv1 = 1.toByte().floorDiv(2.toByte())
const val byteFloorDiv2 = 2.toByte().floorDiv(2.toByte())
const val byteFloorDiv3 = 3.toByte().floorDiv(2.toByte())
const val byteFloorDiv4 = (-1).toByte().floorDiv(2.toByte())

const val shortFloorDiv1 = 1.toShort().floorDiv(2.toShort())
const val shortFloorDiv2 = 2.toShort().floorDiv(2.toShort())
const val shortFloorDiv3 = 3.toShort().floorDiv(2.toShort())
const val shortFloorDiv4 = (-1).toShort().floorDiv(2.toShort())

const val intFloorDiv1 = 1.floorDiv(2)
const val intFloorDiv2 = 2.floorDiv(2)
const val intFloorDiv3 = 3.floorDiv(2)
const val intFloorDiv4 = (-1).floorDiv(2)

const val longFloorDiv1 = 1L.floorDiv(2L)
const val longFloorDiv2 = 2L.floorDiv(2L)
const val longFloorDiv3 = 3L.floorDiv(2L)
const val longFloorDiv4 = (-1L).floorDiv(2L)

const val byteMod1 = 1.toByte().mod(2.toByte())
const val byteMod2 = 2.toByte().mod(2.toByte())
const val byteMod3 = 3.toByte().mod(2.toByte())
const val byteMod4 = (-1).toByte().mod(2.toByte())

const val shortMod1 = 1.toShort().mod(2.toShort())
const val shortMod2 = 2.toShort().mod(2.toShort())
const val shortMod3 = 3.toShort().mod(2.toShort())
const val shortMod4 = (-3).toShort().mod(2.toShort())

const val intMod1 = 1.mod(2)
const val intMod2 = 2.mod(2)
const val intMod3 = 3.mod(2)
const val intMod4 = (-1).mod(2)

const val longMod1 = 1L.mod(2L)
const val longMod2 = 2L.mod(2L)
const val longMod3 = 3L.mod(2L)
const val longMod4 = (-1L).mod(2L)

const val floatMod1 = 1.0f.mod(2.0f)
const val floatMod2 = 2.0f.mod(2.0f)
const val floatMod3 = 3.0f.mod(2.0f)
const val floatMod4 = (-1.0f).mod(2.0f)

const val doubleMod1 = 1.0.mod(2.0)
const val doubleMod2 = 2.0.mod(2.0)
const val doubleMod3 = 3.0.mod(2.0)
const val doubleMod4 = (-1.0).mod(2.0)


fun box(): String {
    if (code.id() != 49) return "Fail 1"

    if (byteFloorDiv1.id() != 0)     return "Fail 2.1 Byte"
    if (byteFloorDiv2.id() != 1)     return "Fail 2.2 Byte"
    if (byteFloorDiv3.id() != 1)     return "Fail 2.3 Byte"
    if (byteFloorDiv4.id() != -1)    return "Fail 2.4 Byte"
    if (shortFloorDiv1.id() != 0)     return "Fail 2.1 Short"
    if (shortFloorDiv2.id() != 1)     return "Fail 2.2 Short"
    if (shortFloorDiv3.id() != 1)     return "Fail 2.3 Short"
    if (shortFloorDiv4.id() != -1)    return "Fail 2.4 Short"
    if (intFloorDiv1.id() != 0)     return "Fail 2.1 Int"
    if (intFloorDiv2.id() != 1)     return "Fail 2.2 Int"
    if (intFloorDiv3.id() != 1)     return "Fail 2.3 Int"
    if (intFloorDiv4.id() != -1)    return "Fail 2.4 Int"
    if (longFloorDiv1.id() != 0L)     return "Fail 2.1 Long"
    if (longFloorDiv2.id() != 1L)     return "Fail 2.2 Long"
    if (longFloorDiv3.id() != 1L)     return "Fail 2.3 Long"
    if (longFloorDiv4.id() != -1L)    return "Fail 2.4 Long"

    if (byteMod1.id() != 1.toByte())       return "Fail 3.1 Byte"
    if (byteMod2.id() != 0.toByte())       return "Fail 3.2 Byte"
    if (byteMod3.id() != 1.toByte())       return "Fail 3.3 Byte"
    if (byteMod4.id() != 1.toByte())       return "Fail 3.4 Byte"
    if (shortMod1.id() != 1.toShort())       return "Fail 3.1 Short"
    if (shortMod2.id() != 0.toShort())       return "Fail 3.2 Short"
    if (shortMod3.id() != 1.toShort())       return "Fail 3.3 Short"
    if (shortMod4.id() != 1.toShort())       return "Fail 3.4 Short"
    if (intMod1.id() != 1)       return "Fail 3.1 Int"
    if (intMod2.id() != 0)       return "Fail 3.2 Int"
    if (intMod3.id() != 1)       return "Fail 3.3 Int"
    if (intMod4.id() != 1)       return "Fail 3.4 Int"
    if (longMod1.id() != 1L)       return "Fail 3.1 Long"
    if (longMod2.id() != 0L)       return "Fail 3.2 Long"
    if (longMod3.id() != 1L)       return "Fail 3.3 Long"
    if (longMod4.id() != 1L)       return "Fail 3.4 Long"
    if (floatMod1.id() != 1.0f)       return "Fail 3.1 Float"
    if (floatMod2.id() != 0.0f)       return "Fail 3.2 Float"
    if (floatMod3.id() != 1.0f)       return "Fail 3.3 Float"
    if (floatMod4.id() != 1.0f)       return "Fail 3.4 Float"
    if (doubleMod1.id() != 1.0)       return "Fail 3.1 Double"
    if (doubleMod2.id() != 0.0)       return "Fail 3.2 Double"
    if (doubleMod3.id() != 1.0)       return "Fail 3.3 Double"
    if (doubleMod4.id() != 1.0)       return "Fail 3.4 Double"

    return "OK"
}
