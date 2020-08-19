// See https://youtrack.jetbrains.com/issue/KT-39206.
@Deprecated("Don't call this\nPlease")
fun myFunc() = 17

// See https://youtrack.jetbrains.com/issue/KT-41193.
@Deprecated(
        level = DeprecationLevel.ERROR,
        message = "This class is deprecated for removal during serialization 1.0 API stabilization.\n" +
                "For configuring Json instances, the corresponding builder function can be used instead, e.g. instead of" +
                "'Json(JsonConfiguration.Stable.copy(isLenient = true))' 'Json { isLenient = true }' should be used.\n" +
                "Instead of storing JsonConfiguration instances of the code, Json instances can be used directly:" +
                "'Json(MyJsonConfiguration.copy(prettyPrint = true))' can be replaced with 'Json(from = MyApplicationJson) { prettyPrint = true }'"
)
public open class JsonConfiguration