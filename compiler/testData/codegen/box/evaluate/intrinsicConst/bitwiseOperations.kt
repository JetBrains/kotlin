// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

fun <T> T.id() = this

const val zeroValByte = 0.toByte()
const val oneValByte = 1.toByte()
const val twoValByte = 2.toByte()
const val threeValByte = 3.toByte()

const val andByte1 = oneValByte.and(twoValByte)
const val andByte2 = twoValByte.and(twoValByte)
const val andByte3 = threeValByte.and(twoValByte)
const val andByte4 = 12.toByte().and(10.toByte())

const val orByte1 = oneValByte.or(twoValByte)
const val orByte2 = twoValByte.or(twoValByte)
const val orByte3 = threeValByte.or(twoValByte)
const val orByte4 = 12.toByte().or(10.toByte())

const val xorByte1 = oneValByte.xor(twoValByte)
const val xorByte2 = twoValByte.xor(twoValByte)
const val xorByte3 = threeValByte.xor(twoValByte)
const val xorByte4 = 12.toByte().xor(10.toByte())

const val invByte1 = zeroValByte.inv()
const val invByte2 = oneValByte.inv()

const val andOrChainedByte = oneValByte.or(twoValByte).and(threeValByte).and(oneValByte)

const val zeroValShort = 0.toShort()
const val oneValShort = 1.toShort()
const val twoValShort = 2.toShort()
const val threeValShort = 3.toShort()

const val andShort1 = oneValShort.and(twoValShort)
const val andShort2 = twoValShort.and(twoValShort)
const val andShort3 = threeValShort.and(twoValShort)
const val andShort4 = 12.toShort().and(10.toShort())

const val orShort1 = oneValShort.or(twoValShort)
const val orShort2 = twoValShort.or(twoValShort)
const val orShort3 = threeValShort.or(twoValShort)
const val orShort4 = 12.toShort().or(10.toShort())

const val xorShort1 = oneValShort.xor(twoValShort)
const val xorShort2 = twoValShort.xor(twoValShort)
const val xorShort3 = threeValShort.xor(twoValShort)
const val xorShort4 = 12.toShort().xor(10.toShort())

const val invShort1 = zeroValShort.inv()
const val invShort2 = oneValShort.inv()

const val andOrChainedShort = oneValShort.or(twoValShort).and(threeValShort).and(oneValShort)

fun box(): String {
    if (andByte1.id() != zeroValByte) return "Fail andByte1"
    if (andByte2.id() != twoValByte) return "Fail andByte2"
    if (andByte3.id() != twoValByte) return "Fail andByte3"
    if (andByte4.id() != 8.toByte()) return "Fail andByte4"

    if (orByte1.id() != threeValByte) return "Fail orByte1"
    if (orByte2.id() != twoValByte) return "Fail orByte2"
    if (orByte3.id() != threeValByte) return "Fail orByte3"
    if (orByte4.id() != 14.toByte()) return "Fail orByte4"

    if (xorByte1.id() != threeValByte) return "Fail xorByte1"
    if (xorByte2.id() != zeroValByte) return "Fail xorByte2"
    if (xorByte3.id() != oneValByte) return "Fail xorByte3"
    if (xorByte4.id() != 6.toByte()) return "Fail xorByte4"

    if (invByte1.id() != (-1).toByte()) return "Fail invByte1"
    if (invByte2.id() != (-2).toByte()) return "Fail invByte2"

    if (andOrChainedByte.id() != 1.toByte()) return "Fail andOrChainedByte"

    if (andShort1.id() != zeroValShort) return "Fail andShort1"
    if (andShort2.id() != twoValShort) return "Fail andShort2"
    if (andShort3.id() != twoValShort) return "Fail andShort3"
    if (andShort4.id() != 8.toShort()) return "Fail andShort4"

    if (orShort1.id() != threeValShort) return "Fail orShort1"
    if (orShort2.id() != twoValShort) return "Fail orShort2"
    if (orShort3.id() != threeValShort) return "Fail orShort3"
    if (orShort4.id() != 14.toShort()) return "Fail orShort4"

    if (xorShort1.id() != threeValShort) return "Fail xorShort1"
    if (xorShort2.id() != zeroValShort) return "Fail xorShort2"
    if (xorShort3.id() != oneValShort) return "Fail xorShort3"
    if (xorShort4.id() != 6.toShort()) return "Fail xorShort4"

    if (invShort1.id() != (-1).toShort()) return "Fail invShort1"
    if (invShort2.id() != (-2).toShort()) return "Fail invShort2"

    if (andOrChainedShort.id() != 1.toShort()) return "Fail andOrChainedShort"

    return "OK"
}
