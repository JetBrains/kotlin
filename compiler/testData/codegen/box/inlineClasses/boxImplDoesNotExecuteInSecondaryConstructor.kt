// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC private constructor(val i: Int) {
    @Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS")
    constructor() : this(0) {
        counter += 1
    }
}

var counter = 0

fun <T> id(t: T) = t

fun box(): String {
    val ic = IC()
    if (counter != 1) return "FAIL 1: $counter"
    counter = 0

    id(ic)
    if (counter != 0) return "FAIL 2: $counter"

    return "OK"
}
