interface I {
    fun toInt(): Int
}

interface J : I

abstract class KotlinNumber : Number() {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("intValue")<!>
    override fun toInt(): Int = 0

    <!INAPPLICABLE_JVM_NAME!>@JvmName("wrongName")<!>
    override fun toByte(): Byte = 0.toByte()
}

abstract class KotlinNumberSpecialBridge : Number(), J {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("longValue")<!>
    override fun toLong(): Long = 0L
}

abstract class KotlinNumberDirectError : Number(), I {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("intValue")<!>
    override fun toInt(): Int = 0
}

abstract class KotlinNumberIndirectError : Number(), J {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("intValue")<!>
    override fun toInt(): Int = 0
}