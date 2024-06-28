annotation class Anno(val s: String)

@Deprecated("property")
@Anno("property")
@set:Deprecated("setter")
var memberP<caret>roperty = 32
    @Deprecated("getter")
    @Anno("getter")
    get() = field
    @Anno("setter")
    @setparam:[Deprecated("setparam") Anno("setparam")]
    set(value) {
        field = value
    }