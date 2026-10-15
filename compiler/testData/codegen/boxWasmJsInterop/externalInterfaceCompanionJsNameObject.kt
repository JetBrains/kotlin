// WITH_STDLIB

@JsName("Object")
@Suppress("COMPANION_OBJECT_IN_EXTERNAL_INTERFACE")
external interface MigratedEnum : JsAny {
    companion object
}

fun isUsableCompanion(value: MigratedEnum.Companion): Boolean =
    js("value !== null && (typeof value === 'object' || typeof value === 'function')")

val MigratedEnum.Companion.ENTRY: MigratedEnum
    get() {
        check(isUsableCompanion(this))
        return "entry".toJsString().unsafeCast<MigratedEnum>()
    }

fun box(): String {
    if (!isUsableCompanion(MigratedEnum.Companion)) return "Fail: companion passed to JS"
    if (MigratedEnum.ENTRY.toString() != "entry") return "Fail: companion extension"
    return "OK"
}
