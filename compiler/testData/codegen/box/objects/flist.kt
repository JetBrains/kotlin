public abstract class FList<T>() {
    public abstract val head: T
    public abstract val tail: FList<T>
    public abstract val empty: Boolean

    default object {
        val emptyFList = object: FList<Any>() {
            public override val head: Any
                get() = throw UnsupportedOperationException();

            public override val tail: FList<Any>
                get() = this

            public override val empty: Boolean
                get() = true
        }
    }

    public fun plus(head: T): FList<T> = object : FList<T>() {
        override public val head: T
            get() = head

        override public val empty: Boolean
            get() = false

        override public val tail: FList<T>
            get() = this@FList
    }
}

public fun <T> emptyFList(): FList<T> = FList.emptyFList as FList<T>

public fun <T> FList<T>.reverse(where: FList<T> = emptyFList<T>()) : FList<T> =
        if(empty) where else tail.reverse(where + head)

public fun <T> FList<T>.iterator(): Iterator<T> = object: Iterator<T> {
    private var cur: FList<T> = this@iterator

    override public fun next(): T {
        val res = cur.head
        cur = cur.tail
        return res
    }
    override public fun hasNext(): Boolean = !cur.empty
}

fun box() : String {
  var r = ""
  for(s in (emptyFList<String>() + "O" + "K").reverse()) {
    r += s
  }
  return r
}
