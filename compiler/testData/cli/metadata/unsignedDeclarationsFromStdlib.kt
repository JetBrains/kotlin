package kotlin

public value class UByte internal constructor(internal val data: Byte) {
    public companion object {
        public const val MIN_VALUE: UByte = UByte(0)
        public const val MAX_VALUE: UByte = UByte(-1)
    }
}

public value class UShort internal constructor(internal val data: Short) {
    public companion object {
        public const val MIN_VALUE: UShort = UShort(0)
        public const val MAX_VALUE: UShort = UShort(-1)
    }
}

public value class UInt internal constructor(internal val data: Int) {
    public companion object {
        public const val MIN_VALUE: UInt = UInt(0)
        public const val MAX_VALUE: UInt = UInt(-1)
    }
}

public value class ULong internal constructor(internal val data: Long) {
    public companion object {
        public const val MIN_VALUE: ULong = ULong(0)
        public const val MAX_VALUE: ULong = ULong(-1)
    }
}
