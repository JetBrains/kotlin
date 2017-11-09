import org.jetbrains.annotations.PropertyKey

private val BUNDLE_NAME = "propertyFileUsages.0"

public fun message(@PropertyKey(resourceBundle = "propertyFileUsages.0") key: String) = key
public fun message2(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = key

fun test() {
    message("foo.bar")
}