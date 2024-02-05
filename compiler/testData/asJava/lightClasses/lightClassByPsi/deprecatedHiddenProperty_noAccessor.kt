package test.pkg

class Test {
    @Deprecated("no more property", level = DeprecationLevel.HIDDEN)
    var pOld_noAccessor_deprecatedOnProperty: String = "42"

    @get:Deprecated("no more getter", level = DeprecationLevel.HIDDEN)
    var pOld_noAccessor_deprecatedOnGetter: String = "42"

    @set:Deprecated("no more setter", level = DeprecationLevel.HIDDEN)
    var pOld_noAccessor_deprecatedOnSetter: String = "42"

    var pNew_noAccessor: String = "42"
}
