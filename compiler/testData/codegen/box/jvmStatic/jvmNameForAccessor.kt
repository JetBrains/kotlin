// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: tt.kt
package tt

class CA {
    companion object {
      @JvmStatic
      @JvmName("uhoh")
      internal fun f(): String = "OK"
    }
}

// FILE: qq.kt

package qq
import tt.CA

fun box() = CA.f()