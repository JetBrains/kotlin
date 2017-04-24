// WITH_RUNTIME

fun foo(map: Map<String, Int>) {
    for ((<caret>_, _) in map) {

    }
}