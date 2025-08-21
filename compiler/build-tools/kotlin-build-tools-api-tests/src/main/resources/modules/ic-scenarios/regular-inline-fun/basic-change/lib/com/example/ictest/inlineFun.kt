package com.example.ictest

class Holder() {
    val item: String = "nice data"

    inline fun regularInlineFun(): String {
        return item
    }
}
