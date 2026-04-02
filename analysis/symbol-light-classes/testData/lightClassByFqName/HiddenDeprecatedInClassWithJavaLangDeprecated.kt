// a.Outer
// LIBRARY_PLATFORMS: JVM
package a

class Outer {
    @Deprecated("f", level = DeprecationLevel.HIDDEN)
    @java.lang.Deprecated
    fun f() {

    }
}