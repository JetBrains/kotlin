// MODE: function_return
val o = object : Iterable<Int> {
    override fun iterator()<# : Iterator<Int> #> = object : Iterator<Int> {
        override fun next()<# : Int #> = 1
        override fun hasNext()<# : Boolean #> = true
    }
}
