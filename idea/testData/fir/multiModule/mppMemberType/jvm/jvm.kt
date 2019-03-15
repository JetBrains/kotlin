actual class MyList {
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

class DerivedWrapper : Wrapper() {
    fun use() {
        // We should deal with receiver to resolve this
        list.get(1)
        list.set(2, 3)
    }
}