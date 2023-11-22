// WITH_STDLIB
// IGNORE_BACKEND: JS_IR NATIVE
// ^ KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone

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
