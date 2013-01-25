public object SomeClass {
    var bug: Any = ""

    private val workerThread = object : Thread() {
        override fun run() {
            try {
              foo()
              bug = "none"
            }
            catch(t: Throwable) {
                bug = t
            }
        }
    }

    {
        workerThread.start()
    }

    private fun foo() : Unit {
    }
}

public fun box():String {
    if(SomeClass.bug is Throwable)
      throw SomeClass.bug as Throwable
    return "OK"
}
