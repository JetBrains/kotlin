import test.*
import Kind.*

enum class Kind {
    LOCAL
    EXTERNAL
    GLOBAL
}

class Internal(val value: String)

class External(val value: String)

class Global(val value: String)

fun test1(intKind: Kind, extKind: Kind): Global {

    var externalResult = doCall @ext {
        () : External ->

        val internalResult = doCall @int {
            () : Internal ->
            if (intKind == Kind.GLOBAL) {
                return@test1 Global("internal -> global")
            } else if (intKind == EXTERNAL) {
                return@ext External("internal -> external")
            }
            return@int Internal("internal -> local")
        }

        if (extKind == GLOBAL || extKind == EXTERNAL) {
            return Global("external -> global")
        }

        External(internalResult.value + ": external -> local");
    }

    return Global(externalResult.value + ": exit")
}

fun box(): String {
    var test1 = test1(LOCAL, LOCAL).value
    if (test1 != "internal -> local: external -> local: exit") return "test1: ${test1}"

    test1 = test1(EXTERNAL, LOCAL).value
    if (test1 != "internal -> external: exit") return "test2: ${test1}"

    test1 = test1(GLOBAL, LOCAL).value
    if (test1 != "internal -> global") return "test3: ${test1}"


    test1 = test1(LOCAL, EXTERNAL).value
    if (test1 != "external -> global") return "test4: ${test1}"

    test1 = test1(EXTERNAL, EXTERNAL).value
    if (test1 != "internal -> external: exit") return "test5: ${test1}"

    test1 = test1(GLOBAL, EXTERNAL).value
    if (test1 != "internal -> global") return "test6: ${test1}"


    test1 = test1(LOCAL, GLOBAL).value
    if (test1 != "external -> global") return "test7: ${test1}"

    test1 = test1(EXTERNAL, GLOBAL).value
    if (test1 != "internal -> external: exit") return "test8: ${test1}"

    test1 = test1(GLOBAL, GLOBAL).value
    if (test1 != "internal -> global") return "test9: ${test1}"


    return "OK"
}
