import java.util.*

fun box(): String {
    val a = ArrayList<String>() as AbstractList<String>
    a.add(null)
    try {
        val b: String = a[0]
        return "Fail: an exception should be thrown"
    } catch (e: IllegalStateException) {
        val st = (e as java.lang.Throwable).getStackTrace()
        if (st.size() < 5) {
            return "Fail: very small stack trace, should at least have current function and JUnit reflective calls: ${Arrays.toString(st)}"
        }
        val top = st[0]
        if (!(top.getClassName() == "PlatformTypeAssertionStackTraceKt" && top.getMethodName() == "box")) {
            return "Fail: top stack trace element should be PlatformTypeAssertionStackTraceKt.box() from default package, but was $top"
        }
        return "OK"
    }
}
