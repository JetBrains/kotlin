// "Create extension function 'get'" "true"
// ERROR: operator modifier is required on 'get' in ''

fun x (y: Any) {
    val z: Any = y<caret>[""]
}
