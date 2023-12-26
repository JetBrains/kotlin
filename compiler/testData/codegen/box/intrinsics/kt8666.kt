// JVM_ABI_K1_K2_DIFF: KT-63861

val MAX_LONG = "9223372036854775807"
val PREFIX = "max = "

fun customToString(prefix: String, l: Long, concat: (String, Long) -> String) = concat(prefix, l)

fun customToString(l: Long, concat: (Long) -> String) = concat(l)

val stringPlus = String::plus
val stringNPlus = String?::plus
val emptyStringPlus = ""::plus
val emptyStringNPlus = ("" as String?)::plus
val prefixPlus = PREFIX::plus
val prefixNPlus = (PREFIX as String?)::plus

fun box(): String {
    if (MAX_LONG != "${Long.MAX_VALUE}") return "fail template"
    if (MAX_LONG != "" + Long.MAX_VALUE) return "fail \"\" +"
    if (MAX_LONG != ("" as String?) + Long.MAX_VALUE) return "fail \"\"? +"
    if (MAX_LONG != "".plus(Long.MAX_VALUE)) return "fail \"\".plus"
    if (MAX_LONG != ("" as String?).plus(Long.MAX_VALUE)) return "fail \"\"?.plus"
    if (MAX_LONG != (String::plus)("", Long.MAX_VALUE)) return "fail String::plus"
//    if (MAX_LONG != stringPlus("", Long.MAX_VALUE)) return "fail stringPlus(\"\", Long.MAX_VALUE)"
//    if (MAX_LONG != customToString("", Long.MAX_VALUE, String::plus)) return "fail customToString(\"\", Long.MAX_VALUE, String::plus)"
    if (MAX_LONG != (String?::plus)("", Long.MAX_VALUE)) return "fail String?::plus"
//    if (MAX_LONG != stringNPlus("", Long.MAX_VALUE)) return "fail stringNPlus(\"\", Long.MAX_VALUE)"
//    if (MAX_LONG != customToString("", Long.MAX_VALUE, String?::plus)) return "fail customToString(\"\", Long.MAX_VALUE, String?::plus)"
    if (MAX_LONG != (""::plus)(Long.MAX_VALUE)) return "fail \"\"::plus"
//    if (MAX_LONG != emptyStringPlus(Long.MAX_VALUE)) return "fail emptyStringPlus(Long.MAX_VALUE)"
//    if (MAX_LONG != customToString(Long.MAX_VALUE, ""::plus)) return "fail customToString(Long.MAX_VALUE, \"\"::plus)"
    if (MAX_LONG != (("" as String?)::plus)(Long.MAX_VALUE)) return "fail \"\"?::plus"
//    if (MAX_LONG != emptyStringNPlus(Long.MAX_VALUE)) return "fail emptyStringPlus(Long.MAX_VALUE)"
//    if (MAX_LONG != customToString(Long.MAX_VALUE, ("" as String?)::plus)) return "fail customToString(Long.MAX_VALUE, (\"\" as String?)::plus)"

    if (PREFIX + MAX_LONG != "max = ${Long.MAX_VALUE}") return "fail template with prefix"
    if (PREFIX + MAX_LONG != PREFIX + Long.MAX_VALUE) return "fail \"$PREFIX\" +"
    if (PREFIX + MAX_LONG != (PREFIX as String?) + Long.MAX_VALUE) return "fail \"$PREFIX\"? +"
    if (PREFIX + MAX_LONG != PREFIX.plus(Long.MAX_VALUE)) return "fail \"$PREFIX\".plus"
    if (PREFIX + MAX_LONG != (PREFIX as String?).plus(Long.MAX_VALUE)) return "fail \"$PREFIX\"?.plus"
    if (PREFIX + MAX_LONG != (String::plus)(PREFIX, Long.MAX_VALUE)) return "fail String::plus(\"$PREFIX\", ...)"
//    if (PREFIX + MAX_LONG != stringPlus(PREFIX, Long.MAX_VALUE)) return "fail stringPlus(\"$PREFIX\", Long.MAX_VALUE)"
//    if (PREFIX + MAX_LONG != customToString(PREFIX, Long.MAX_VALUE, String::plus)) return "fail customToString(\"$PREFIX\", Long.MAX_VALUE, String::plus)"
    if (PREFIX + MAX_LONG != (String?::plus)(PREFIX, Long.MAX_VALUE)) return "fail String?::plus(\"$PREFIX\", ...)"
//    if (PREFIX + MAX_LONG != stringNPlus(PREFIX, Long.MAX_VALUE)) return "fail stringNPlus(\"$PREFIX\", Long.MAX_VALUE)"
//    if (PREFIX + MAX_LONG != customToString(PREFIX, Long.MAX_VALUE, String?::plus)) return "fail customToString(\"$PREFIX\", Long.MAX_VALUE, String?::plus)"
    if (PREFIX + MAX_LONG != (PREFIX::plus)(Long.MAX_VALUE)) return "fail \"$PREFIX\"::plus"
//    if (PREFIX + MAX_LONG != prefixPlus(Long.MAX_VALUE)) return "fail prefixPlus(Long.MAX_VALUE)"
//    if (PREFIX + MAX_LONG != customToString(Long.MAX_VALUE, PREFIX::plus)) return "fail customToString(Long.MAX_VALUE, \"$PREFIX\"::plus)"
    if (PREFIX + MAX_LONG != ((PREFIX as String?)::plus)(Long.MAX_VALUE)) return "fail \"$PREFIX\"?::plus"
//    if (PREFIX + MAX_LONG != prefixNPlus(Long.MAX_VALUE)) return "fail prefixNPlus(Long.MAX_VALUE)"
//    if (PREFIX + MAX_LONG != customToString(Long.MAX_VALUE, (PREFIX as String?)::plus)) return "fail customToString(Long.MAX_VALUE, \"$PREFIX\"?::plus)"

    return "OK"
}
