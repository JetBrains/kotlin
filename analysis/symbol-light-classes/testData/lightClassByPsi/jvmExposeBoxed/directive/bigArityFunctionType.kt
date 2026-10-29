// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(val value: Long)

class NamedFn : (
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
) -> String {
    override fun invoke(
        a1: String,
        a2: Id,
        a3: Id,
        a4: Id,
        a5: Id,
        a6: Id,
        a7: Id,
        a8: Id,
        a9: Id,
        a10: Id,
        a11: Id,
        a12: Id,
        a13: Id,
        a14: Id,
        a15: Id,
        a16: Id,
        a17: Id,
        a18: Id,
        a19: Id,
        a20: Id,
        a21: Id,
        a22: Id,
        a23: Id,
    ): String = a1
}

// LIGHT_ELEMENTS_NO_DECLARATION: Id.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], NamedFn.class[invoke-t09OwRc]
