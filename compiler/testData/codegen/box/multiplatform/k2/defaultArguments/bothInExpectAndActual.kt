// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

public expect fun <T> Array<out T>.copyInto(
    destination: Array<T>, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size
): Array<T>

// MODULE: platform()()(common)
// FILE: platform.kt

// This test should be updated once KT-22818 is fixed; default values are not allowed in the actual function
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun <T> Array<out T>.copyInto(
    destination: Array<T>, destinationOffset: Int = 42, startIndex: Int = 43, endIndex: Int = size + 44
): Array<T> {
    destination as Array<Int>
    destination[0] = destinationOffset
    destination[1] = startIndex
    destination[2] = endIndex
    return destination
}

fun box(): String {
    val a = Array<Int>(3) { it }
    val result = a.copyInto(a)
    return if (result[0] == 42 && result[1] == 43 && result[2] == 47) "OK"
           else "Fail: ${result[0]} ${result[1]} ${result[2]}"
}
