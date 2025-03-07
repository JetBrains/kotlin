// LIBRARY_PLATFORMS: JVM

class C @JvmOverloads constructor(
    val type: String?,
    val p1: Boolean = false,
    val p2: String = type!!
) {
    @JvmOverloads
    fun foo(x: Int = 1, y: Double, z: String = "") {}
    @JvmOverloads
    fun bar(x: Int = 1, y: Double = 1.3, z: String = "") {}
    @JvmOverloads
    fun baz(x: Int = 1, y: Double = 1.3, z: String) {}
    @JvmOverloads
    fun foobar(x: Int, y: Double = 1.3, z: String = "") {}
    @JvmOverloads
    fun foobarbaz(x: Int, y: Double = 1.3, z: String) {}

    @JvmOverloads
    constructor(x: Int, vararg strings: String, double: Double = 1.0, b: Boolean = false) : this(null)

    companion object {
        @JvmOverloads
        fun foo123(x: Int = 1, y: Double, z: String = "") {}
        @JvmStatic
        @JvmOverloads
        fun fooStatic(x: Int = 1, y: Double, z: String = "") {}
    }

    @JvmOverloads
    fun varargWithOneDefault(x: Int, vararg strings: String, double: Double = 1.0) {

    }

    @JvmOverloads
    fun varargWithTwoDefaults(x: Int, vararg strings: String, double: Double = 1.0, b: Boolean = false) {

    }

    @JvmOverloads
    fun varargWithDefaultInMiddle(x: Int, vararg strings: String, double: Double = 1.0, b: Boolean) {

    }

    @JvmOverloads
    fun varargWithDefaultsBefore(x: Int, double: Double = 1.0, b: Boolean = false, vararg strings: String) {

    }
}
