val MAX_LONG = "9223372036854775807"
val PREFIX = "max = "

fun box(): String {
    if (MAX_LONG != "${Long.MAX_VALUE}") return "fail template"
    if (MAX_LONG != "" + Long.MAX_VALUE) return "fail \"\" +"
    if (MAX_LONG != ("" as String?) + Long.MAX_VALUE) return "fail \"\"? +"
    if (MAX_LONG != "".plus(Long.MAX_VALUE)) return "fail \"\".plus"
    if (MAX_LONG != ("" as String?).plus(Long.MAX_VALUE)) return "fail \"\"?.plus"
    if (MAX_LONG != (String::plus)("", Long.MAX_VALUE)) return "fail String::plus"
    if (MAX_LONG != (String?::plus)("", Long.MAX_VALUE)) return "fail String?::plus"
    if (MAX_LONG != (""::plus)(Long.MAX_VALUE)) return "fail \"\"::plus"
    if (MAX_LONG != (("" as String?)::plus)(Long.MAX_VALUE)) return "fail \"\"?::plus"

    if (PREFIX + MAX_LONG != "max = ${Long.MAX_VALUE}") return "fail template with prefix"
    if (PREFIX + MAX_LONG != PREFIX + Long.MAX_VALUE) return "fail \"$PREFIX\" +"
    if (PREFIX + MAX_LONG != (PREFIX as String?) + Long.MAX_VALUE) return "fail \"$PREFIX\"? +"
    if (PREFIX + MAX_LONG != PREFIX.plus(Long.MAX_VALUE)) return "fail \"$PREFIX\".plus"
    if (PREFIX + MAX_LONG != (PREFIX as String?).plus(Long.MAX_VALUE)) return "fail \"$PREFIX\"?.plus"
    if (PREFIX + MAX_LONG != (String::plus)(PREFIX, Long.MAX_VALUE)) return "fail String::plus($PREFIX, ...)"
    if (PREFIX + MAX_LONG != (String?::plus)(PREFIX, Long.MAX_VALUE)) return "fail String?::plus($PREFIX, ...)"
    if (PREFIX + MAX_LONG != (PREFIX::plus)(Long.MAX_VALUE)) return "fail \"$PREFIX\"::plus"
    if (PREFIX + MAX_LONG != ((PREFIX as String?)::plus)(Long.MAX_VALUE)) return "fail \"$PREFIX\"?::plus"

    return "OK"
}
