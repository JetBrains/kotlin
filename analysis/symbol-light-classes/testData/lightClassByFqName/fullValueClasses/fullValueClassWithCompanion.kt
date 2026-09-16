// pack.StringWrapper
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

value class StringWrapper(val s: String, val n: Int) {
    companion object {
        @JvmStatic
        fun unwrap(s: StringWrapper): String = s.s

        @JvmStatic
        fun regularStaticFunction() {}

        @JvmStatic
        var staticVariable: StringWrapper get() = StringWrapper("OK", 1)
            set(value) {

            }

        @JvmStatic
        var regularStaticVariable: Int get() = 0
            set(value) {

            }
    }
}
