fun foo(p: java.util.HashMap<String, java.io.File>){ }

fun bar(o: Any){
    foo(o as <caret>)
}