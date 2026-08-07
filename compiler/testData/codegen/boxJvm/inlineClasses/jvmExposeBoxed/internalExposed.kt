// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// 'internal' is not private API, so exposure is allowed. The boxed variant carries the same module-suffixed
// name the unboxed one has - the bytecode listing is the assertion, because the suffix depends on the module
// name and cannot be spelled out in a Java caller.
@JvmExposeBoxed
internal fun internalTopLevel(id: Id): String = id.value

class Holder {
    @JvmExposeBoxed
    internal fun internalMember(id: Id): String = id.value
}

fun box(): String {
    var res = internalTopLevel(Id("OK"))
    if (res != "OK") return "FAIL 1: $res"
    res = Holder().internalMember(Id("OK"))
    if (res != "OK") return "FAIL 2: $res"
    return "OK"
}
