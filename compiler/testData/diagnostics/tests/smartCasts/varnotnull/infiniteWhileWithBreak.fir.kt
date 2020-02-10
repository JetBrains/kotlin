data class SomeObject(val n: SomeObject?) {
    fun doSomething(): Boolean = true
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject) {
    var e: SomeObject?
    e = start
    // This comparison is senseless
    while (e != null) {
        // Smart cast because of the loop condition
        if (!e.doSomething())
            break
        // We still have smart cast here despite of a break
        e = e.next()
    } 
    // e can be null because of next()
    e.doSomething()
}