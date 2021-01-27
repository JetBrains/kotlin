// WITH_RUNTIME
// FILE: 1.kt
// KJS_WITH_FULL_RUNTIME
package test
inline fun stub() {

}

// FILE: 2.kt

fun box(): String {
    return "KO".reversed()
}
