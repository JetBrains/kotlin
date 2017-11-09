// "Change to 'return@foo'" "true"

fun foo(f:()->Int){}

fun bar() {
    foo {
        return<caret> 1
    }
}