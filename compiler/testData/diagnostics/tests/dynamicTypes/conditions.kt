// MODULE[js]: m1
// FILE: k.kt

fun test(d: dynamic, b: Boolean?) {
    if (d) {}
    while (d) {}
    do {} while (d)

    if (d || false) {}
    if (d && true) {}
    if (d ?: true) {}
    if (b ?: d) {}
}