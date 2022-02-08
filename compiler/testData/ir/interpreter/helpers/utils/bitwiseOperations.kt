package kotlin

public inline infix fun Byte.and(other: Byte): Byte = (this.toInt() and other.toInt()).toByte()
public inline infix fun Byte.or(other: Byte): Byte = (this.toInt() or other.toInt()).toByte()
public inline infix fun Byte.xor(other: Byte): Byte = (this.toInt() xor other.toInt()).toByte()
public inline fun Byte.inv(): Byte = (this.toInt().inv()).toByte()

public inline infix fun Short.and(other: Short): Short = (this.toInt() and other.toInt()).toShort()
public inline infix fun Short.or(other: Short): Short = (this.toInt() or other.toInt()).toShort()
public inline infix fun Short.xor(other: Short): Short = (this.toInt() xor other.toInt()).toShort()
public inline fun Short.inv(): Short = (this.toInt().inv()).toShort()


