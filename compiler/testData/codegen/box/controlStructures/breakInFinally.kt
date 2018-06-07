// IGNORE_BACKEND: JS_IR
fun box(): String {
    while (true) {
        try {
            continue;
        }
        finally {
            break;
        }
    }
    return "OK"
}