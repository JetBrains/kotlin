// ISSUE: KT-89859
// WITH_STDLIB
// IGNORE_BACKEND: WASM_WASI, WASM_JS
//                 ^^^ KT-88829

// MODULE: lib
// FILE: lib.kt
import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> storeEntries(arr: Array<Any?>) {
    arr[0] = enumEntries<T>()
}

inline fun <reified T> storeAs(arr: Array<Any?>, x: Any?) {
    arr[0] = x as T
}

inline fun <reified T> storeSafeAs(arr: Array<Any?>, x: Any?) {
    arr[0] = x as? T
}

// MODULE: main(lib)
// FILE: main.kt
import kotlin.test.*

enum class E { A, B }

fun box(): String {
    val arr = arrayOfNulls<Any>(1)

    storeEntries<E>(arr)
    assertEquals("[A, B]", arr[0].toString())

    storeSafeAs<String>(arr, 42)
    assertNull(arr[0])

    try {
        storeAs<String>(arr, null)
        return "Fail: no exception"
    } catch (e: NullPointerException) {
    }

    return "OK"
}
