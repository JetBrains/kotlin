// WITH_RUNTIME

fun main() {
    foo(true);
}
fun foo(someBool:Boolean) {
    if (someBool) {
        println("test1");
    } else if (<caret>false) {
        println("test2");
    }
    println("test3");
}