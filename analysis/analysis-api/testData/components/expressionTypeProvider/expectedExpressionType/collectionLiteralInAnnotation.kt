enum class MyEnum {
    FIRST,
    SECOND
}
annotation class MyAnnotation(vararg val enum: MyEnum)
@MyAnnotation(enum = [<caret>])
fun method() { }