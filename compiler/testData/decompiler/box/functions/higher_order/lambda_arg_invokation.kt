fun lambdaReceiver(body: () -> Unit) {
    body()
}

fun box(): String {
    lambdaReceiver { println() }
    return "OK"
}