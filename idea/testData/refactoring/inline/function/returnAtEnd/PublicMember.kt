class My {
    fun run() {
        val foo = <caret>doThing()
        System.out.println(foo)
    }

    fun doThing(): Int {
        val foo = 1 + 2
        return foo
    }
}