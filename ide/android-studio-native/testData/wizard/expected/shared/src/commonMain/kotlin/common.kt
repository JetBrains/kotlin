package org.jetbrains.shared

expect class Platform() {
    val platform: String
}


class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}
