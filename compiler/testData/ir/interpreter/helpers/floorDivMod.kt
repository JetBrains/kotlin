package kotlin

public inline fun Int.mod(other: Int): Int {
    val r = this % other
    return r + (other and (((r xor other) and (r or -r)) shr 31))
}

public inline fun Long.mod(other: Long): Long {
    val r = this % other
    return r + (other and (((r xor other) and (r or -r)) shr 63))
}
