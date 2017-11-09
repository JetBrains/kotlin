import org.jetbrains.annotations.PropertyKey

public fun message(@PropertyKey(resourceBundle = "propertyUsages.0") key: String) = key

public fun String.infixMessage(@PropertyKey(resourceBundle = "propertyUsages.0") key: String) = key

public fun @receiver:PropertyKey(resourceBundle = "propertyUsages.0") String.infixMessage2(s: String) = this

public fun @receiver:PropertyKey(resourceBundle = "propertyUsages.0") String.unaryMinus() = this

public fun Int.get(@PropertyKey(resourceBundle = "propertyUsages.0") key: String) = this

public fun @receiver:PropertyKey(resourceBundle = "propertyUsages.0") String.get(s: String) = this

fun test() {
    @PropertyKey(resourceBundle = "propertyUsages.0") val s1 = "foo.bar"
    @PropertyKey(resourceBundle = "propertyUsages.0") val s2 = "foo.baz"
    message("foo.bar")
    message("foo.baz")

    "test" infixMessage "foo.bar"
    "foo.bar" infixMessage "test"
    "foo.bar" infixMessage2 "test"
    "test" infixMessage2 "foo.bar"
    "foo.bar".infixMessage2("test")
    -"foo.bar"
    1["foo.bar"]
    "foo.bar"["test"]
    "test"["foo.bar"]
}