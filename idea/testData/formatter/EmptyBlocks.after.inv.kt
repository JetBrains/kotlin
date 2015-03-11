class Test {}

class Other {
    default object {}
}

object TestObject {}

val testVal = {}

val testObject = object {}

fun foo(f: (String) -> Unit = {}) = {}

fun call() {
    foo({})
    foo {}
}

class Test1 {
}

class Other1 {
    default object {
    }
}

object TestObject1 {
}

val testVal1 = { }

val testObject1 = object {
}

fun foo1(f: (String) -> Unit = { }) = { }

fun call1() {
    foo1({ })
    foo1 { }
}