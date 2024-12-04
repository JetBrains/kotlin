@WithEnumFromBody("", WithEnumFromBody.MyLevel.ERROR)
public annotation class WithEnumFromBody cons<caret>tructor(
    val a: String = "",
    @WithEnumFromBody("", WithEnumFromBody.MyLevel.WARNING) val b: MyLevel = MyLevel.ERROR
) {
    public enum class MyLevel {
        WARNING,
        ERROR,
    }
}
