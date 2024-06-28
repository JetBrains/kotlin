// FIR_DUMP
// ISSUE: KT-48799

class C {
    val v1: String
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = ""

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    val v2 = ""

    var v3: String
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = ""
        set(value) {}

    var v4: String
        get() = ""
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        set(value) {
        }

    var v5: String
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = ""
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        set(value) {
        }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    var v6: String
        get() = ""
        set(value) {}
}

val v1: String = ""
val v2: String = ""
var v3: String = ""
var v4: String = ""
var v5: String = ""
var v6: String = ""

fun test(c: C) {
    with (c) {
        v1  // FE1.0: Resolves to C.v1, FIR: Resolves to top-level v1
        v2  // FE1.0/FIR: Resolves to top-level v2
        v3  // FE1.0: Resolves to C.v3, FIR: Resolves to top-level v3
        v3 = ""  // FE1.0/FIR: Resolves to C.v3
        v4  // FE1.0/FIR: Resolves to C.v4
        v4 = ""  // FE1.0: Resolves to C.v4, FIR: Resolves to top-level v4
        v5  // FE1.0: Resolves to C.v5, FIR: resolves to top-level v5
        v5 = ""  // FE1.0: Resolves to C.v5, FIR: resolves to top-level v5
        v6  // FE1.0/FIR: Resolves to top-level v6
        v6 = ""  // FE1.0/FIR: Resolves to top-level v6
    }
}
