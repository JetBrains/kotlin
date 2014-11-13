import java.util.ArrayList

val paramTest = 12

fun small(paramFirst: ArrayList<String>, paramSecond: Comparable<java.lang.RuntimeException>) {
}

fun test() = small(<caret>)

// EXIST: {"lookupString":"paramSecond","tailText":" Comparable<RuntimeException>","itemText":"paramSecond ="}
// EXIST: {"lookupString":"paramFirst","tailText":" ArrayList<String>","itemText":"paramFirst ="}
