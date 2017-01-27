
typealias TaRunnable = Runnable


fun usesRunnable(runnable: Runnable) {

}

fun usage() {
    usesRunnable(<caret>)
}

// EXIST: {"lookupString":"TaRunnable","tailText":"() (<root>)","typeText":"Runnable"}