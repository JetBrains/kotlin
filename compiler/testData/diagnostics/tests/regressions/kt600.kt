//KT-600 Problem with 'sure' extension function type inference

fun <T : Any> T?._sure() : T { if (this != null) return <!DEBUG_INFO_SMARTCAST!>this<!> else throw NullPointerException() }

fun test() {
    val i : Int? = 10
    val <!UNUSED_VARIABLE!>i2<!> : Int = i._sure() // inferred type is Int? but Int was excepted
}
