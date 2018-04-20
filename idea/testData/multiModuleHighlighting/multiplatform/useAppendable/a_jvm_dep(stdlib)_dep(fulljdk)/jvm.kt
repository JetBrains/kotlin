package foo

fun tryIt() {
    buildString {  // kotlin.text.StringBuilder = java.lang.StringBuilder
        appendMe()
    }
}