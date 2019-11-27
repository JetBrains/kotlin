// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

public object SomeObject {
    private val workerThread = object : Thread() {
        override fun run() {
            foo()
        }
    }

    init {
        workerThread.start()
    }

    private fun foo() : Unit {
    }
}

public class SomeClass() {
    inner class Inner {
        val copy = list
    }

    private val list = ArrayList<String>()
    var status : Throwable? = null
    private val workerThread = object : Thread() {
        public override fun run() {
            try {
                list.add("123")
                list.add("33")
                Inner().copy.add("444")
            }
            catch(t: Throwable) {
                status = t
            }
        }
    }

    init {
        workerThread.start()
        workerThread.join()
    }
}

public fun box(): String {
    var obj = SomeClass()
    return if (obj.status == null) "OK" else {
        (obj.status as java.lang.Throwable).printStackTrace()
        "failed"
    }
}
