// WITH_STDLIB
package jvm

fun test(x: List<Int>?) {
    // If the function returns false, the value is definitely not null:
    if (!x.isNullOrEmpty()) {
        println(x.size) // Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type List<Int>?
    }
}

fun test(x: Any?) {
    // If the function returns (does not throw), then the argument is true:
    require(x is String)
    println(x.length) // Unresolved reference: length
}
