import base.*

interface SubCheck : Check {
    override fun test(): String {
        return "OK"
    }
}

class <!EXPLICIT_OVERRIDE_REQUIRED_IN_MIXED_MODE("public open fun test(): String defined in SubCheckClass", "public open fun test(): String defined in base.CheckClass", "all-compatibility")!>SubCheckClass<!> : CheckClass(), SubCheck