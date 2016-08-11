import java.util.ArrayList

val paramTest = 12

fun small(paramFirst: ArrayList<String>, paramSecond: Comparable<kotlin.collections.List<kotlin.Any>>) {
}

fun test() = small(<caret>)

// EXIST: {"lookupString":"paramSecond","tailText":" Comparable<List<Any>>","itemText":"paramSecond ="}
// EXIST: {"lookupString":"paramFirst","tailText":" ArrayList<String>","itemText":"paramFirst ="}
