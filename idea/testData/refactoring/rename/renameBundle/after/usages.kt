import org.jetbrains.annotations.PropertyKey

private val BUNDLE_NAME = "BarBundle"

public fun message(@PropertyKey(resourceBundle = "BarBundle") key: String) = key
public fun message2(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = key

fun test() {
    message("foo.bar")
}