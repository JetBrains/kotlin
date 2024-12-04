package test.pkg

class Test {
    @Deprecated("no more property", level = DeprecationLevel.HIDDEN)
    var pOld_accessors_deprecatedOnProperty: String? = null
        get() = field ?: "null?"
        set(value) {
            if (field == null) {
                field = value
            }
        }

    @get:Deprecated("no more getter", level = DeprecationLevel.HIDDEN)
    var pOld_accessors_deprecatedOnGetter: String? = null
        get() = field ?: "null?"
        set(value) {
            if (field == null) {
                field = value
            }
        }

    @set:Deprecated("no more setter", level = DeprecationLevel.HIDDEN)
    var pOld_accessors_deprecatedOnSetter: String? = null
        get() = field ?: "null?"
        set(value) {
            if (field == null) {
                field = value
            }
        }

    var pNew_accessors: String? = null
        get() = field ?: "null?"
        set(value) {
            if (field == null) {
                field = value
            }
        }
}