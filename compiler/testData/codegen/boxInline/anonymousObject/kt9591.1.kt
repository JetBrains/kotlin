import test.*

public fun box(): String {
    var z = "fail"
    inlineFun {
        val obj = object  {
            val _delegate by lazy {
                z = "OK"
            }
        }

        obj._delegate
    }

    return z;
}