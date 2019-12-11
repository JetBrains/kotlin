// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNREACHABLE_CODE
//KT-2445 Calling method with function with generic parameter causes compile-time exception
package a

fun main() {
    test {

    }
}

fun <R> test(callback: (R) -> Unit):Unit = callback(null!!)