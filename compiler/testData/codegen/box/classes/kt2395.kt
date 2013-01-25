import java.util.AbstractList

class MyList(): AbstractList<String>() {
     public fun getModificationCount(): Int = modCount
     public override fun get(index: Int): String = ""
     public override fun size(): Int = 0
}

fun box(): String {
    return if (MyList().getModificationCount() == 0) "OK" else "fail"
}
