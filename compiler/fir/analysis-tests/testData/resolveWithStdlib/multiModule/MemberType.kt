// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class MyList {
    fun get(i: Int): Int
}

open class Wrapper(val list: MyList)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual open class MyList {
    actual fun get(i: Int): Int = i

    fun set(i: Int, v: Int) {}
}

class DerivedList : MyList() {
    fun useMember() {
        get(1)
        set(2, 3)
    }
}

fun useList(list: MyList) {
    // We should deal with receiver to resolve this
    list.get(1)
    list.set(2, 3)
}

class DerivedWrapper : Wrapper(MyList()) {
    fun use() {
        // We should deal with receiver to resolve this
        list.get(1)
        list.set(2, 3)
    }
}
