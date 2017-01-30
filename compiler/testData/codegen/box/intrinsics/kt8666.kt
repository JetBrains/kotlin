val maxLong = "9223372036854775807"

fun box(): String {
    if (maxLong != "${Long.MAX_VALUE}") return "fail template"
    if (maxLong != "" + Long.MAX_VALUE) return "fail \"\" +"
    if (maxLong != "".plus(Long.MAX_VALUE)) return "fail \"\".plus"
    if (maxLong != (String::plus)("", Long.MAX_VALUE)) return "fail String::plus"
    if (maxLong != (""::plus)(Long.MAX_VALUE)) return "fail \"\"::plus"
    return "OK"
}
