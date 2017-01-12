// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun box(): String {
    var cycle = true;
    while (true) {
        if (true || throw java.lang.RuntimeException()) {
            return "OK"
        }
    }
    return "fail"
}