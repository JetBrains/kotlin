data class SomeObject(val n: SomeObject?) {
    fun doSomething() {}
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject) {
    var e: SomeObject? = start
    while (e != null) {
        // While condition makes both smart casts possible
        e.doSomething()
        e = e.next()
    }
}