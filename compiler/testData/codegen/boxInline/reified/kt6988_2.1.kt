import test.*

fun box(): String {
    "yo".inlineMeIfYouCan<StringBuilder>()().run()
    return "OK"
}