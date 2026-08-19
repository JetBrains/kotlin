// ISSUE: KT-88571
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^ @AssociatedObjectKey is not available in Kotlin/JVM

// WITH_STDLIB
// ONLY_IR_DCE

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class ObjectKey(val kClass: KClass<*>)

// JS/Wasm: The enum entries keep `static_init` alive, and `static_init` is what constructs the companion object.
//   So the associated object survives DCE while its `getInstance` function does not.
@ObjectKey(MODE.Companion::class)
enum class MODE {
    ALL;

    companion object
}

fun box(): String {
    // JS/Wasm: Touches an entry only, never the MODE companion, so `MODE$Companion$getInstance` stays unreachable.
    return if (MODE.ALL.toString() == "ALL") "OK" else "FAIL"
}
