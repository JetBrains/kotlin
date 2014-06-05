import test.*
import Kind.*

enum class Kind {
    LOCAL
    EXTERNAL
    GLOBAL
}

class Holder {
    var value: String = ""
}

val FINALLY_CHAIN = "in local finally, in external finally, in global finally"

class Internal(val value: String)

class External(val value: String)

class Global(val value: String)

fun test1(intKind: Kind, extKind: Kind, holder: Holder): Global {
    holder.value = ""
    try {
        var externalResult = doCall @ext {
            (): External ->

            try {
                val internalResult = doCall @int {
                    (): Internal ->
                    try {
                        if (intKind == Kind.GLOBAL) {
                            return@test1 Global("internal -> global")
                        }
                        else if (intKind == EXTERNAL) {
                            return@ext External("internal -> external")
                        }
                        return@int Internal("internal -> local")
                    }
                    finally {
                        holder.value += "in local finally"
                    }
                }

                if (extKind == GLOBAL || extKind == EXTERNAL) {
                    return Global("external -> global")
                }

                External(internalResult.value + ": external -> local");

            }
            finally {
                holder.value += ", in external finally"
            }
        }

        return Global(externalResult.value + ": exit")
    }
    finally {
        holder.value += ", in global finally"
    }


}

fun box(): String {
    var holder = Holder()

    var test1 = test1(LOCAL, LOCAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> local: external -> local: exit") return "test1: ${test1},  finally = ${holder.value}"

    test1 = test1(EXTERNAL, LOCAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> external: exit") return "test2: ${test1},  finally = ${holder.value}"

    test1 = test1(GLOBAL, LOCAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> global") return "test3: ${test1},  finally = ${holder.value}"


    test1 = test1(LOCAL, EXTERNAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "external -> global") return "test4: ${test1},  finally = ${holder.value}"

    test1 = test1(EXTERNAL, EXTERNAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> external: exit") return "test5: ${test1},  finally = ${holder.value}"

    test1 = test1(GLOBAL, EXTERNAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> global") return "test6: ${test1},  finally = ${holder.value}"


    test1 = test1(LOCAL, GLOBAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "external -> global") return "test7: ${test1},  finally = ${holder.value}"

    test1 = test1(EXTERNAL, GLOBAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> external: exit") return "test8: ${test1},  finally = ${holder.value}"

    test1 = test1(GLOBAL, GLOBAL, holder).value
    if (holder.value != FINALLY_CHAIN || test1 != "internal -> global") return "test9: ${test1},  finally = ${holder.value}"


    return "OK"
}
