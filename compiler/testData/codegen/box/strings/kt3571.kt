// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class Thing(delegate: CharSequence) : CharSequence by delegate
  
fun box(): String {
    val l = Thing("hello there").length
    return if (l == 11) "OK" else "Fail $l"
}
