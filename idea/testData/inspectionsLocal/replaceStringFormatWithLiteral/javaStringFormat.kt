// RUNTIME_WITH_FULL_JDK

fun test() {
    val foo = 1
    val bar = 2

    <caret>java.lang.String.format("foo is %s, bar is %s.", foo, bar)
}