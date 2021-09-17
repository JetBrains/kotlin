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
        <!DEPRECATION_ERROR!>v1<!>  // DEPRECATION_ERROR in FE 1.0, see KT-48799
        v2
        <!DEPRECATION_ERROR!>v3<!>  // DEPRECATION_ERROR in FE 1.0, see KT-48799
        v3 = ""
        v4
        <!DEPRECATION_ERROR!>v4<!> = ""  // DEPRECATION_ERROR in FE 1.0, see KT-48799
        <!DEPRECATION_ERROR!>v5<!>  // DEPRECATION_ERROR in FE 1.0, see KT-48799
        <!DEPRECATION_ERROR!>v5<!> = ""  // DEPRECATION_ERROR in FE 1.0, see KT-48799
        v6
        v6 = ""
    }
}
