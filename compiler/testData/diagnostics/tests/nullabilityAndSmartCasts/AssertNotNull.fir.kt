// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun main() {
    val a : Int? = null
    val b : Int? = null
    checkSubtype<Int>(a!!)
    a!! + 2
    a!!.plus(2)
    a!!.plus(b!!)
    2.plus(b!!)
    2 + b!!

    val c = 1
    c!!

    val d : Any? = null

    if (d != null) {
        d!!
    }

    // smart cast isn't needed, but is reported due to KT-4294
    if (d is String) {
        d!!
    }

    if (d is String?) {
        if (d != null) {
            d!!
        }
        if (d is String) {
            d!!
        }
    }

    val f : String = a!!
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><String>(a!!)
}