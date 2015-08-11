package p

import _protected

fun box(): String {
    return KotlinClass().ok()
}

class KotlinClass : _protected() {
    fun ok() = ok
}
