package test.pkg

class Test {
    @Deprecated("no more property", level = DeprecationLevel.HIDDEN)
    var pOld_setter_deprecatedOnProperty: String? = null
        set(value) {
            if (field == null) {
                field = value
            }
        }

    @get:Deprecated("no more getter", level = DeprecationLevel.HIDDEN)
    var pOld_setter_deprecatedOnGetter: String? = null
        set(value) {
            if (field == null) {
                field = value
            }
        }

    @set:Deprecated("no more setter", level = DeprecationLevel.HIDDEN)
    var pOld_setter_deprecatedOnSetter: String? = null
        set(value) {
            if (field == null) {
                field = value
            }
        }

    var pNew_setter: String? = null
        set(value) {
            if (field == null) {
                field = value
            }
        }
}