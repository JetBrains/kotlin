package p

import _protectedSetter

fun box(): String {
    return KotlinClass().ok()
}

class KotlinClass : _protectedSetter() {
    fun ok(): String {
        x = "o"
        x += "k"
        return x
    }
}
