// IGNORE_BACKEND_K1: ANY
// ^^^ Context parameters aren't going to be supported in K1.
// LANGUAGE: +ContextParameters
// MODULE: properties_with_context_parameters

package properties_with_context_parameters.test

context(c1: Int) val regularValProperty: String
    get() = ""
context(c1: Int) val Int.regularValProperty: String
    get() = ""
context(c1: Int) val Long.regularValProperty: String
    get() = ""
context(c1: Int) val Number.regularValProperty: String
    get() = ""
context(c1: Int) var regularVarProperty: String
    get() = ""
    set(value) {}

context(c1: Int, c2: Long) val regularValProperty: String
    get() = ""
context(c1: Int, c2: Long) val Int.regularValProperty: String
    get() = ""
context(c1: Int, c2: Long) val Long.regularValProperty: String
    get() = ""
context(c1: Int, c2: Long) val Number.regularValProperty: String
    get() = ""
context(c1: Int, c2: Long) var regularVarProperty: String
    get() = ""
    set(value) {}

class FunctionContainer {
    context(c1: Int) val regularValProperty: String
        get() = ""
    context(c1: Int) val Int.regularValProperty: String
        get() = ""
    context(c1: Int) val Long.regularValProperty: String
        get() = ""
    context(c1: Int) val Number.regularValProperty: String
        get() = ""
    context(c1: Int) var regularVarProperty: String
        get() = ""
        set(value) {}

    context(c1: Int, c2: Long) val regularValProperty: String
        get() = ""
    context(c1: Int, c2: Long) val Int.regularValProperty: String
        get() = ""
    context(c1: Int, c2: Long) val Long.regularValProperty: String
        get() = ""
    context(c1: Int, c2: Long) val Number.regularValProperty: String
        get() = ""
    context(c1: Int, c2: Long) var regularVarProperty: String
        get() = ""
        set(value) {}
}
