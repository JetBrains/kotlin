// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun nil() = null

fun list() = java.util.Arrays.asList("1")

fun box(): String {
    for (x in nil()?:list()) {
    }
    return "OK"
}
