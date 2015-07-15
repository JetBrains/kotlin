fun getInterface(): GenericInterface<String> {
    return GenericInterface { d, i, j, s ->
        "OK"
    }
}

fun box(): String {
    return getInterface().foo(0.0, 0, 0, 0)
}
