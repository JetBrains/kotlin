// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

interface IntConvertible {
    fun toInt(): Int
}

abstract class KotlinAbstractNumber : Number(), IntConvertible

class KotlinNumber : KotlinAbstractNumber() {
    override fun toByte(): Byte = 1
    override fun toDouble(): Double = 1.0
    override fun toFloat(): Float = 1.0f
    override fun toInt(): Int = 1
    override fun toLong(): Long = 1L
    override fun toShort(): Short = 1
}

interface CharContainer {
    operator fun get(index: Int): Char
}

interface KotlinCharSequenceContainer : CharSequence, CharContainer

class KotlinCharSequence : KotlinCharSequenceContainer {
    override val length: Int get() = 2

    override fun get(index: Int): Char = "OK"[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = "OK".subSequence(startIndex, endIndex)
}

fun box(): String {
    assertEquals(1, KotlinAbstractNumber::class.members.single { it.name == "toInt" }.call(KotlinNumber()))
    assertEquals(1, KotlinNumber::class.members.single { it.name == "toInt" }.call(KotlinNumber()))
    assertEquals('K', KotlinCharSequenceContainer::class.members.single { it.name == "get" }.call(KotlinCharSequence(), 1))
    assertEquals('K', KotlinCharSequence::class.members.single { it.name == "get" }.call(KotlinCharSequence(), 1))

    return "OK"
}
