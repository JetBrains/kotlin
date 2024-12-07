fun box(): String {
    Klass({})
    return "OK"
}

class Klass {
    constructor(func: (() -> Unit)?) {
        if (func != null) {
            func()
        }
    }
}
