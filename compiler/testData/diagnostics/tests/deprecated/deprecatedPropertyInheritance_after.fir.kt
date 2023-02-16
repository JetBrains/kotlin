// LANGUAGE: +StopPropagatingDeprecationThroughOverrides
package foo

interface HiddenDeprecated {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    var p: Int
}

interface NoDeprecation {
    var p: Int
}


open class WarningDeprecated {
    @Deprecated("", level = DeprecationLevel.WARNING)
    open var p: Int = 3
}

open class ErrorDeprecated {
    @Deprecated("", level = DeprecationLevel.ERROR)
    open var p: Int = 3
}

open class GetterDeprecated {
    open var p: Int = 3
        @Deprecated("") get
}

open class SetterDeprecated {
    open var p: Int = 3
        @Deprecated("") set
}

class WD: WarningDeprecated() {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {}
}

class ED: ErrorDeprecated() {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class GD: GetterDeprecated() {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class SD: SetterDeprecated() {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class SDH: SetterDeprecated(), HiddenDeprecated {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class EDH: ErrorDeprecated(), HiddenDeprecated {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class NED: ErrorDeprecated(), NoDeprecation {
    override var <!OVERRIDE_DEPRECATION!>p<!>: Int
        get() = 3
        set(value) {
        }
}

class Diff {
    @Deprecated("", level = DeprecationLevel.WARNING)
    var p: Int
        @Deprecated("", level = DeprecationLevel.ERROR) get() = 3
        @Deprecated("", level = DeprecationLevel.HIDDEN) set(value) {

        }
}

fun use(
        warningDeprecated: WarningDeprecated, errorDeprecated: ErrorDeprecated, setterDeprecated: SetterDeprecated,
        getterDeprecated: GetterDeprecated, hiddenDeprecated: HiddenDeprecated,
        wd: WD, ed: ED, gd: GD, sd: SD,
        sdh: SDH, edh: EDH, ned: NED,
        diff: Diff
) {
    warningDeprecated.<!DEPRECATION!>p<!>
    warningDeprecated.<!DEPRECATION!>p<!> = 1

    errorDeprecated.<!DEPRECATION_ERROR!>p<!>
    errorDeprecated.<!DEPRECATION_ERROR!>p<!> = 1

    getterDeprecated.<!DEPRECATION!>p<!>
    getterDeprecated.p = 1

    setterDeprecated.p
    setterDeprecated.<!DEPRECATION!>p<!> = 1

    hiddenDeprecated.<!UNRESOLVED_REFERENCE!>p<!>
    hiddenDeprecated.<!UNRESOLVED_REFERENCE!>p<!> = 1

    wd.p
    wd.p = 1

    ed.p
    ed.p = 1

    gd.p
    gd.p = 1

    sd.p
    sd.p = 1

    sdh.p
    sdh.p = 1

    edh.p
    edh.p = 1

    ned.p
    ned.p = 1

    diff.<!DEPRECATION_ERROR!>p<!>
    diff.<!UNRESOLVED_REFERENCE!>p<!> = 1
}
