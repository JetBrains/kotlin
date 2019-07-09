expect class MyList {
    fun get(i: Int): Int
}

open class Wrapper(val list: MyList)