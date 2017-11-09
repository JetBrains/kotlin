
fun nyCompiler() {
    try {
        return
    }
    catch (e: Exception) {}
    finally {
        try {} catch (e: Exception) {}
    }
}


fun nyCompiler2() {
    try {
        return
    }
    finally {
        try {} catch (e: Exception) {}
    }
}

fun box(): String {
    nyCompiler()
    nyCompiler2()
    return "OK"
}