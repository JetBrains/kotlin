fun foo(p: java.io.File?){ }

fun bar(o: Any){
    foo(o as? <caret>)
}