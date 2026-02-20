// WITH_STDLIB
// FILE: 1.kt
package test
inline fun stub() {

}

// FILE: 2.kt

fun box(): String {
    return "KO".reversed()
}
