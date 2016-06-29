// "Create extension function 'Any.get'" "true"
// WITH_RUNTIME

fun x (y: Any) {
    val z: Any = y<caret>[""]
}
