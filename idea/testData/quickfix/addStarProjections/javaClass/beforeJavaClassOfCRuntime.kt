// "class org.jetbrains.kotlin.idea.quickfix.AddStarProjectionsFix" "false"
class C

fun test() {
    javaClass<C<caret>>()
}
