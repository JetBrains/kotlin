// pack.StringWrapper
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

@JvmInline
value class StringWrapper(val s: String) {
    companion object {
        @JvmStatic
        fun unwrap(s: StringWrapper): String = s.s

        @JvmStatic
        fun regularStaticFunction() {}

        @JvmStatic
        var staticVariable: StringWrapper get() = StringWrapper("OK")
            set(value) {

            }

        @JvmStatic
        var regularStaticVariable: Int get() = 0
            set(value) {

            }
    }
}
