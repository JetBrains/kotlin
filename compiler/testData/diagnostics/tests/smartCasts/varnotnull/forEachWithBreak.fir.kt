// !WITH_NEW_INFERENCE
data class SomeObject(val n: SomeObject?) {
    fun doSomething() {}
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject): SomeObject {
    var e: SomeObject? = start
    for (i in 0..42) {
        if (e == null)
            break
        // Smart casts are possible because of the break before
        e.doSomething()
        e = e.next()
    }
    return <!RETURN_TYPE_MISMATCH!>e<!>
}