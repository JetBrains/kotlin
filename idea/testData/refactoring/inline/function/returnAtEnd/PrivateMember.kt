class My {
    fun run() {
        // TODO: should be available
        val foo = <caret>doThing()
        System.out.println(foo)
    }

    private fun doThing(): Int {
        val foo = 1 + 2
        return foo
    }
}