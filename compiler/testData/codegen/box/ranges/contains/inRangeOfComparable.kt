// WITH_RUNTIME

fun box(): String {
    if ("z" in "Alpha" .. "Omega") return "Fail 1"
    if ("Gamma" !in "Alpha" .. "Omega") return "Fail 2"
    return "OK"
}