// WITH_STDLIB
val int get() = 1
val string get() = "Hello"
val boolean get() = true
val double get() = 1.0
val float get() = 1.0f
val char get() = 'A'
val long get() = 1L
val uint get() = 1u
val ulong get() = 1UL
val ulong2 get() = 0xFFFF_FFFF_FFFFu

val intExplicit: Int? get(): Int? = 1
val stringExplicit: String? get(): String? = "Hello"
val booleanExplicit: Boolean? get(): Boolean? = true
val doubleExplicit: Double? get(): Double? = 1.0
val floatExplicit: Float? get(): Float? = 1.0f
val charExplicit: Char? get(): Char? = 'A'
val longExplicit: Long? get(): Long? = 1L
val uintExplicit: UInt? get(): UInt? = 1u
val ulongExplicit: ULong? get(): ULong? = 1UL
val ulong2Explicit: ULong? get(): ULong? = 0xFFFF_FFFF_FFFFu