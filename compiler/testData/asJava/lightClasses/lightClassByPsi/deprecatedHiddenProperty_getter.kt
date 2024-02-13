package test.pkg

class Test {
    @Deprecated("no more property", level = DeprecationLevel.HIDDEN)
    var pOld_getter_deprecatedOnProperty: String? = null
        get() = field ?: "null?"

    @get:Deprecated("no more getter", level = DeprecationLevel.HIDDEN)
    var pOld_getter_deprecatedOnGetter: String? = null
        get() = field ?: "null?"

    @set:Deprecated("no more setter", level = DeprecationLevel.HIDDEN)
    var pOld_getter_deprecatedOnSetter: String? = null
        get() = field ?: "null?"

    var pNew_getter: String? = null
        get() = field ?: "null?"
}