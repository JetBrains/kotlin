// LANGUAGE: +CompanionBlocksAndExtensions

class Box<T>(val item: T)

companion fun Box.create(s: String) = Box(s)
companion val Box.defaultValue = "default"

fun box(): String {
    val b = Box.create("hello")
    if (b.item != "hello") return "FAIL: item=${b.item}"

    if (Box.defaultValue != "default") return "FAIL: defaultValue=${Box.defaultValue}"

    return "OK"
}
