// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.test.assertEquals

class KotlinCharSequence : CharSequence {
    override val length: Int = 2
    override fun get(index: Int): Char = "OK"[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = "OK".subSequence(startIndex, endIndex)
}

class KotlinNumber : Number() {
    override fun toByte(): Byte = 1
    override fun toChar(): Char = '1'
    override fun toDouble(): Double = 1.0
    override fun toFloat(): Float = 1.0f
    override fun toInt(): Int = 1
    override fun toLong(): Long = 1L
    override fun toShort(): Short = 1
}

fun box(): String {
    assertEquals(
        "fun test.KotlinCharSequence.get(kotlin.Int): kotlin.Char",
        KotlinCharSequence::class.members.single { it.name == "get" }.toString(),
    )
    assertEquals(
        "val test.KotlinCharSequence.length: kotlin.Int",
        KotlinCharSequence::class.members.single { it.name == "length" }.toString(),
    )
    assertEquals(
        "fun test.KotlinCharSequence.subSequence(kotlin.Int, kotlin.Int): kotlin.CharSequence",
        KotlinCharSequence::class.members.single { it.name == "subSequence" }.toString(),
    )
    assertEquals('O', KotlinCharSequence::get.call(KotlinCharSequence(), 0))
    assertEquals(2, KotlinCharSequence::length.get(KotlinCharSequence()))

    assertEquals(
        "fun test.KotlinNumber.toInt(): kotlin.Int",
        KotlinNumber::class.members.single { it.name == "toInt" }.toString(),
    )
    assertEquals(1, KotlinNumber::toInt.call(KotlinNumber()))

    return "OK"
}
