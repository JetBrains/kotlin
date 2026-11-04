// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

class Wrapper(var baseUrl: String)

enum class ConfigurationParameter {
    BASE_URL(
        { value, nc ->
            println("Base url updated from config parameters " + nc.baseUrl + " -> " + value)
            nc.baseUrl = value
        }
    );

    constructor(apply: (String, Wrapper) -> Unit) {}
}
