// !LANGUAGE: -ReadDeserializedContracts -UseCallsInPlaceEffect
// See KT-18698

val s = mutableListOf<String>()

fun test(name: String?, flag: Boolean): Boolean {
    try {
        name?.let {
            if (flag) {
                s.add(it)
            }
            else {
                s.remove(it)
            }

            return true
        }

        return false
    } finally {
        name?.hashCode()
    }
}