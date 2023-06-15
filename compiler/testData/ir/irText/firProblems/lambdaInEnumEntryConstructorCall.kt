// WITH_STDLIB
// DUMP_LOCAL_DECLARATION_SIGNATURES
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// KT-50774

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57427

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
