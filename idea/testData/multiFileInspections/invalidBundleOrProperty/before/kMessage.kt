import org.jetbrains.annotations.PropertyKey

object K {
    fun message(@PropertyKey(resourceBundle = "TestBundle") key: String, vararg args: Any) = key
    fun message2(@PropertyKey(resourceBundle = "TestBundle") key: String, n: Int, vararg args: Any) = key
}