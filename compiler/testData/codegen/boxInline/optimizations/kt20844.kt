// WITH_STDLIB
// FILE: 1.kt
package test

data class Address(
        val createdTimeMs: Long = 0,
        val firstName: String = "",
        val lastName: String = ""
)

inline fun String.switchIfEmpty(provider: () -> String): String {
    return if (isEmpty()) provider() else this
}

// FILE: 2.kt

import test.*

fun box(): String {
    val address = Address()
    val result = address.copy(
            firstName = address.firstName.switchIfEmpty { "O" },
            lastName = address.lastName.switchIfEmpty { "K" }
    )

    return result.firstName + result.lastName
}
