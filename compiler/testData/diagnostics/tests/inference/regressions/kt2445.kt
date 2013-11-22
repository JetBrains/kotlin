//KT-2445 Calling method with function with generic parameter causes compile-time exception
package a

fun main(args: Array<String>) {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test<!> {

    }
}

// callback is unused due to KT-4233
fun test<R>(<!UNUSED_PARAMETER!>callback<!>: (R) -> Unit):Unit = <!UNREACHABLE_CODE!>callback(null!!)<!>