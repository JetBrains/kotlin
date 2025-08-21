// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class A {
    context(a: String)
    fun test1(): String {
        return "A.test1 "
    }

    fun String.test2(): String {
        return "A.test2 "
    }

    fun test3(): String {
        return "A.test3 "
    }

    fun OnlyDispatch.usage(): String {
        return with("") { test1() } +   //resolves to OnlyDispatch.test1
                with("") { test2() }    //resolves to A.test2
    }

    fun Extension.usage(): String {
        return with("") { test1() }     //resolves to Extension.test1
    }

    fun ContextReceiver.usage(): String {
        return with("") { test1() } +   //resolves to ContextReceiver.test1
                with("") { test2() } +  //resolves to A.test2
                with("") { test3() }    //resolves to ContextReceiver.test3
    }

    fun ContextWithAnotherType.usage(): String {
        return with("") { test1() }     //resolves to A.test1
    }

}

class OnlyDispatch {
    fun test1(): String {
        return "OnlyDispatch.test1 "
    }

    fun test2(): String {
        return "OnlyDispatch.test2 "
    }
}

class Extension {
    fun String.test1(): String {
        return "Extension.test1 "
    }
}

class ContextReceiver {
    context(a: String)
    fun test1(): String {
        return "ContextReceiver.test1 "
    }

    context(a: String)
    fun test2(): String {
        return "ContextReceiver.test2 "
    }

    context(a: String)
    fun test3(): String {
        return "ContextReceiver.test3 "
    }
}

class ContextWithAnotherType {
    context(a: Int)
    fun test1(): String {
        return "ContextWithAnotherType.test1 "
    }
}

fun box(): String {
    var result = "OK"
    with(A()) {
        with(OnlyDispatch()) {
            if (usage() != "OnlyDispatch.test1 A.test2 ") result = "not OK"
        }
        with(Extension()) {
            if (usage() != "Extension.test1 ") result = "not OK"
        }
        with(ContextReceiver()) {
            if (usage() != "ContextReceiver.test1 A.test2 ContextReceiver.test3 ") result = "not OK"
        }
        with(ContextWithAnotherType()) {
            if (usage() != "A.test1 ") result = "not OK"
        }
    }
    return result
}