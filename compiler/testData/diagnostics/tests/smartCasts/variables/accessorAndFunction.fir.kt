class My {

    val y: Int
        get() {
            var x: Int?
            x = 3
            return x.hashCode()
        }

    fun test() {
        var x: Int?
        x = 2
        x.hashCode()
        fun bb() {
           var x: Any?
           x = 4
           x.hashCode()
        }
        x = 4
        // Really smart cast is possible but name shadowing by bb() prevents it
        x.hashCode()
    }
}