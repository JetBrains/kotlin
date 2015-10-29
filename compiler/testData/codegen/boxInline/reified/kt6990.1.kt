import test.*

class OK

fun box(): String {
    return inlineMeIfYouCan<OK>()!!
}