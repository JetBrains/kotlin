package test.pkg

interface TestInterface {
    @Deprecated("no more property", level = DeprecationLevel.HIDDEN)
    var pOld_deprecatedOnProperty: Int

    @get:Deprecated("no more getter", level = DeprecationLevel.HIDDEN)
    var pOld_deprecatedOnGetter: Int

    @set:Deprecated("no more setter", level = DeprecationLevel.HIDDEN)
    var pOld_deprecatedOnSetter: Int

    var pNew: Int
}