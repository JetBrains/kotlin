// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNREACHABLE_CODE
//KT-2445 Calling method with function with generic parameter causes compile-time exception
package a

fun main(args: Array<String>) {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!> {

    }
}

fun <R> test(callback: (R) -> Unit):Unit = callback(null!!)