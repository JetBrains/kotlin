// TARGET_BACKEND: WASM
// WASM_STANDALONE
// ^^^ in non-standalone run, test classes will be placed in a sub-package, so `::class.qualifiedName` would give different result

class NonLocal
val nonLocalObject = object {}

fun box(): String {

    class Local
    if (Local::class.qualifiedName != null) return "Fail1"
    if (Local::class.simpleName != "Local") return "Fail2"

    val localObject = object {}
    if (localObject::class.qualifiedName != null) return "Fail3"
    if (localObject::class.simpleName != null) return "Fail4"

    if (NonLocal::class.qualifiedName != "NonLocal") return "Fail5"
    if (NonLocal::class.simpleName != "NonLocal") return "Fail6"

    if (nonLocalObject::class.qualifiedName != null) return "Fail7"
    if (nonLocalObject::class.simpleName != null) return "Fail8"

    return "OK"
}
