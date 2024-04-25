// DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: +JvmStaticInInterface
// JVM_TARGET: 1.8

interface I {
    companion object {
        @Synchronized fun syncFun() {}

        @Synchronized @JvmStatic fun syncFunJvmStatic() {}

        var syncProp: String
            @Synchronized get() = ""
            @Synchronized set(value) {}

        @JvmStatic var syncPropJvmStatic: String
            @Synchronized get() = ""
            @Synchronized set(value) {}

        var syncPropJvmStaticAccessors: String
            @Synchronized @JvmStatic get() = ""
            @Synchronized @JvmStatic set(value) {}
    }
}
