import a.foo
import a.inlineOnly

fun box(): String {
    if (!a.inlineOnly<String>("OK")) return "fail 1"
   return foo { "OK" }
}
