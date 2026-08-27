// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(val value: Long)

class Test {
    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
    fun <T> query(
        mapper: (
            String,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
            Id,
        ) -> T,
    ): T = "OK" as T

    fun run(): String = query {
            a1,
            a2,
            a3,
            a4,
            a5,
            a6,
            a7,
            a8,
            a9,
            a10,
            a11,
            a12,
            a13,
            a14,
            a15,
            a16,
            a17,
            a18,
            a19,
            a20,
            a21,
            a22,
            a23,
        ->
        a1
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Id.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
