// LANGUAGE: -StopPropagatingDeprecationThroughOverrides
package foo

interface WarningDeprecated {
    @Deprecated("", level = DeprecationLevel.WARNING)
    fun f() {

    }
}

interface ErrorDeprecated {
    @Deprecated("", level = DeprecationLevel.ERROR)
    fun f() {

    }
}

interface HiddenDeprecated {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun f() {

    }
}

interface NotDeprecated {
    fun f() {

    }
}

open class WE : WarningDeprecated, ErrorDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class WH : WarningDeprecated, HiddenDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class EH : ErrorDeprecated, HiddenDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class NW : WarningDeprecated, NotDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class NE : ErrorDeprecated, NotDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class NH : HiddenDeprecated, NotDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class WEH: WarningDeprecated, ErrorDeprecated, HiddenDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

open class NWEH: NotDeprecated, WarningDeprecated, ErrorDeprecated, HiddenDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {

    }
}

class WE2: WE()

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class NWE2<!>: WE(), NotDeprecated

class NWE3: WE(), NotDeprecated {
    override fun f() {
    }
}

interface E2: ErrorDeprecated
interface W2: WarningDeprecated

interface EW2: E2, W2 {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {
    }
}

interface HEW2: EW2, HiddenDeprecated {
    override fun <!OVERRIDE_DEPRECATION!>f<!>() {
    }
}

interface ExplicitError: HEW2 {
    @Deprecated("", level = DeprecationLevel.ERROR)
    override fun f() {
        super.f()
    }
}

fun use(
        wd: WarningDeprecated, ed: ErrorDeprecated, hd: HiddenDeprecated,
        we: WE, wh: WH, eh: EH, nw: NW, ne: NE, nh: NH,
        weh: WEH, nweh: NWEH,
        we2: WE2, nwe2: NWE2, nwe3: NWE3,
        e2: E2, w2: W2, ew2: EW2, hew2: HEW2,
        explicitError: ExplicitError
) {
    wd.<!DEPRECATION!>f<!>()
    ed.<!DEPRECATION_ERROR!>f<!>()
    hd.<!UNRESOLVED_REFERENCE!>f<!>()

    we.f()
    wh.f()
    eh.f()

    nw.f()
    ne.f()
    nh.f()

    weh.f()
    nweh.f()

    we2.f()
    nwe2.f()
    nwe3.f()

    e2.<!DEPRECATION_ERROR!>f<!>()
    w2.<!DEPRECATION!>f<!>()
    ew2.f()
    hew2.f()

    explicitError.<!DEPRECATION_ERROR!>f<!>()
}
