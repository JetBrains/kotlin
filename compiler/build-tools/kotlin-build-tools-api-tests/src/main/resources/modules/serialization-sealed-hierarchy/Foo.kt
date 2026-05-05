package org.example

import kotlinx.serialization.Serializable

@Serializable
sealed class Foo(
    val bar: String,
)
