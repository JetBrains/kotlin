val a1: Int = <!INTEGER_OVERFLOW!>32000.toShort() * 32000.toShort() * 32000.toShort()<!>
val a2: Int = <!INTEGER_OVERFLOW!>128.toByte() * 128.toByte() * 128.toByte() * 128.toByte() * 128.toByte()<!>