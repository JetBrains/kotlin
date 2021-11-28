// WITH_STDLIB
// KT-44496

class C {
    val todo: String = TODO()

    val uninitializedVal: String

    var uninitializedVar: String
}

class Foo {
    init {
        TODO()
    }

    val uninitializedVal: String

    var uninitializedVar: String
}

class Bar {
    val initializedVal = 43

    init {
        TODO()
    }

    val uninitializedVal: String

    var uninitializedVar: String
}

fun box(): String {
    try {
        C()
        return "Fail"
    } catch (e: NotImplementedError) {
        //OK
    }

    try {
        Foo()
        return "Fail"
    } catch (e: NotImplementedError) {
        //OK
    }

    try {
        Bar()
        return "Fail"
    } catch (e: NotImplementedError) {
        //OK
    }

    return "OK"
}
