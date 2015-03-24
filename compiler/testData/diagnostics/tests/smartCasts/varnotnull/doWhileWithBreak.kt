data class SomeObject(val n: SomeObject?) {
    fun doSomething(): Boolean = true
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject) {
    var e: SomeObject?
    e = start
    do {
        // In theory smart cast is possible here
        // But in practice we have a loop with changing e
        // ?: should we "or" entrance type info with condition type info?
        if (!e<!UNSAFE_CALL!>.<!>doSomething())
            break
        // Smart cast here is still not possible
        e = e<!UNSAFE_CALL!>.<!>next()
    } while (e != null)
    // e can be null because of next()
    e<!UNSAFE_CALL!>.<!>doSomething()
}