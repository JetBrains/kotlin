// WITH_REFLECT
// WITH_STDLIB
// ISSUE: KT-86728
import kotlin.reflect.KClass

// Records which type argument the compiler reified at the call site.
var reifiedAs: KClass<*>? = null

inline fun <reified T : Any> decode(): T? {
    reifiedAs = T::class
    return null
}

sealed interface Snapshot {
    data class Real(val x: Int) : Snapshot
    data class Unknown(val type: String) : Snapshot
    companion object {
        val UNKNOWN = Unknown("unknown")
    }
}

fun box(): String {
    // Control: elvis directly in expected-type (`Snapshot`) position.
    reifiedAs = null
    val direct: Snapshot = decode() ?: Snapshot.UNKNOWN
    val directT = reifiedAs?.simpleName
    if (directT != "Snapshot") return "expected direct T=Snapshot, got $directT"

    // Regression: identical elvis, only difference is the surrounding run { }.
    reifiedAs = null
    val viaRun: Snapshot = run { decode() ?: Snapshot.UNKNOWN }
    val runT = reifiedAs?.simpleName
    if (runT != "Snapshot") return "expected run {} T=Snapshot, got $runT"

    return "OK"
}
