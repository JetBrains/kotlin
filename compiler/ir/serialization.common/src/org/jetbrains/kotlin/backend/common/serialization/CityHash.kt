/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("NAME_SHADOWING")

package org.jetbrains.kotlin.backend.common.serialization

private val k0 = 0xc3a5c85c97cb3127U
private val k1 = 0xb492b66fbe98f273U
private val k2 = 0x9ae16a3b2f90404fU
private val kMul = 0x9ddfea08eb382d69UL

private fun toLongLE(b: ByteArray, i: Int): Long {
    return (b[i + 7].toLong() shl 56) +
            ((b[i + 6].toInt() and 255).toLong() shl 48) +
            ((b[i + 5].toInt() and 255).toLong() shl 40) +
            ((b[i + 4].toInt() and 255).toLong() shl 32) +
            ((b[i + 3].toInt() and 255).toLong() shl 24) +
            (b[i + 2].toInt() and 255 shl 16).toLong() +
            (b[i + 1].toInt() and 255 shl 8).toLong() +
            (b[i + 0].toInt() and 255 shl 0).toLong()
}

private fun toIntLE(b: ByteArray, i: Int): Int {
    return (b[i + 3].toInt() and 255 shl 24) +
            (b[i + 2].toInt() and 255 shl 16) +
            (b[i + 1].toInt() and 255 shl 8) +
            (b[i + 0].toInt() and 255 shl 0)
}

private fun fetch64(s: ByteArray, pos: Int): ULong {
    return toLongLE(s, pos).toULong()
}

private fun fetch32(s: ByteArray, pos: Int): UInt {
    return toIntLE(s, pos).toUInt()
}

private fun rotate(value: ULong, shift: Int): ULong {
    return if (shift == 0) value else (value shr shift) or (value shl (64 - shift))
}

private fun shiftMix(value: ULong): ULong {
    return value xor (value shr 47)
}

private fun hashLen16(u: ULong, v: ULong): ULong {
    return hash128to64(u, v)
}

private fun hashLen16(u: ULong, v: ULong, mul: ULong): ULong {
    var a = (u xor v) * mul
    a = a xor (a shr 47)
    var b = (v xor a) * mul
    b = b xor (b shr 47)
    b *= mul
    return b
}

private fun hash128to64(u: ULong, v: ULong): ULong {
    var a = (u xor v) * kMul
    a = a xor (a shr 47)
    var b = (v xor a) * kMul
    b = b xor (b shr 47)
    b *= kMul
    return b
}

private fun hashLen0to16(s: ByteArray, pos: Int, len: Int): ULong {
    if (len >= 8) {
        val mul =  k2 + len.toULong() * 2u
        val a = fetch64(s, pos + 0) + k2
        val b = fetch64(s, pos + len - 8)
        val c = rotate(b, 37).toULong() * mul.toULong() + a.toULong()
        val d = (rotate(a, 25) + b) * mul
        return hashLen16(c, d, mul)
    }
    if (len >= 4) {
        val mul = k2 + len.toULong() * 2u
        val a = fetch32(s, pos).toULong()
        return hashLen16((a shl 3) + len.toUInt(), fetch32(s, pos + len - 4).toULong(), mul)
    }
    if (len > 0) {
        val a = s[pos + 0].toUByte()
        val b = s[pos + len.ushr(1)].toUByte()
        val c = s[pos + len - 1].toUByte()
        val y = a + (b.toUInt() shl 8)
        val z = len.toULong() + (c.toUInt() shl 2)
        return shiftMix(y * k2 xor z * k0) * k2
    }
    return k2
}

private fun hashLen17to32(s: ByteArray, pos: Int, len: Int): ULong {
    val mul = k2 + (len * 2).toUInt()
    val a = fetch64(s, pos + 0) * k1
    val b = fetch64(s, pos + 8)
    val c = fetch64(s, pos + len - 8) * mul
    val d = fetch64(s, pos + len - 16) * k2
    return hashLen16(
            rotate(a + b, 43) + rotate(c, 30) + d,
            a + rotate(b + k2, 18) + c, mul)
}

private fun weakHashLen32WithSeeds(w: ULong, x: ULong, y: ULong, z: ULong, a: ULong, b: ULong): ULongArray {
    var a = a
    var b = b

    a += w
    b = rotate(b + a + z, 21)
    val c = a
    a += x
    a += y
    b += rotate(a, 44)
    return ulongArrayOf(a + z, b + c)
}

private fun weakHashLen32WithSeeds(s: ByteArray, pos: Int, a: ULong, b: ULong): ULongArray {
    return weakHashLen32WithSeeds(
            fetch64(s, pos + 0),
            fetch64(s, pos + 8),
            fetch64(s, pos + 16),
            fetch64(s, pos + 24),
            a, b)
}

fun bswap(value: ULong): ULong {
    val b1 = (value shr 0) and 0xffu
    val b2 = (value shr 8) and 0xffu
    val b3 = (value shr 16) and 0xffu
    val b4 = (value shr 24) and 0xffu
    val b5 = (value shr 32) and 0xffu
    val b6 = (value shr 40) and 0xffu
    val b7 = (value shr 48) and 0xffu
    val b8 = (value shr 56) and 0xffu

    return (b1 shl 56) or (b2 shl 48) or (b3 shl 40) or (b4 shl 32) or
            (b5 shl 24) or (b6 shl 16) or (b7 shl 8) or (b8 shl 0)
}

private fun hashLen33to64(s: ByteArray, pos: Int, len: Int): ULong {
    val mul = k2 + (len * 2).toUInt()
    var a = fetch64(s, pos + 0) * k2
    var b = fetch64(s, pos + 8)
    val c = fetch64(s, pos + len - 24)
    val d = fetch64(s, pos + len - 32)
    val e = fetch64(s, pos + 16) * k2
    val f = fetch64(s, pos + 24) * 9u
    val g = fetch64(s, pos + len - 8)
    val h = fetch64(s, pos + len - 16) * mul
    val u = rotate(a + g, 43) + (rotate(b, 30) + c) * 9u
    val v = ((a + g) xor d) + f + 1u
    val w = bswap((u + v) * mul) + h
    val x = rotate(e + f, 42) + c
    val y = (bswap((v + w) * mul) + g) * mul
    val z = e + f + c

    a = bswap((x + z) * mul + y) + b
    b = shiftMix((z + a) * mul + d + h) * mul

    return b + x

}

public fun cityHash64(s: ByteArray, pos: Int = 0, len: Int = s.size): ULong {
    var pos = pos
    var len = len

    if (len <= 32) {
        return if (len <= 16) {
            hashLen0to16(s, pos, len)
        } else {
            hashLen17to32(s, pos, len)
        }
    } else if (len <= 64) {
        return hashLen33to64(s, pos, len)
    }

    var x = fetch64(s, pos + len - 40)
    var y = fetch64(s, pos + len - 16) + fetch64(s, pos + len - 56)
    var z = hashLen16(fetch64(s, pos + len - 48) + len.toUInt(), fetch64(s, pos + len - 24))

    var v = weakHashLen32WithSeeds(s, pos + len - 64, len.toULong(), z)
    var w = weakHashLen32WithSeeds(s, pos + len - 32, y + k1, x)
    x = x * k1 + fetch64(s, pos + 0)

    len = (len - 1) and 63.inv()
    do {
        x = rotate(x + y + v[0] + fetch64(s, pos + 8), 37) * k1
        y = rotate(y + v[1] + fetch64(s, pos + 48), 42) * k1
        x = x xor w[1]
        y += v[0] + fetch64(s, pos + 40)
        z = rotate(z + w[0], 33) * k1
        v = weakHashLen32WithSeeds(s, pos + 0, v[1] * k1, x + w[0])
        w = weakHashLen32WithSeeds(s, pos + 32, z + w[1], y + fetch64(s, pos + 16))
        run {
            val swap = z
            z = x
            x = swap
        }
        pos += 64
        len -= 64
    } while (len != 0)

    return hashLen16(
            hashLen16(v[0], w[0]) + shiftMix(y) * k1 + z,
            hashLen16(v[1], w[1]) + x
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
fun String.cityHash64(): Long =
    cityHash64(this.toByteArray()).toLong()
