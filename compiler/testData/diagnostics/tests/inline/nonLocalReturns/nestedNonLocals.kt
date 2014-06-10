import Kind.EXT_RETURN
import Kind.GLOBAL_RETURN

enum class Kind {
    LOCAL
    EXT_RETURN
    GLOBAL_RETURN
}

class Internal(val value: String)

class External(val value: String)

class Global(val value: String)

fun test1(intKind: Kind, extKind: Kind): Global {

    var externalResult = doCall @ext {
        () : External ->

        val internalResult = doCall @int {
            () : Internal ->
            if (intKind == Kind.LOCAL) {
                return@test1 Global("internal to global")
            } else if (intKind == EXT_RETURN) {
                return@ext External("internal to external")
            }
            return@int Internal("internal to local")
        }

        if (extKind == GLOBAL_RETURN || extKind == EXT_RETURN) {
            return Global("external to global")
        }

        External(internalResult.value + " to local");
    }

    return Global(externalResult.value + " to exit")
}

public inline fun <R> doCall(block: ()-> R) : R {
    return block()
}
