import java.util.Arrays
import java.util.ArrayList

fun box(): String {
    val a = ArrayList<String>()
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
        if (!(top.getClassName().startsWith("_DefaultPackage") && top.getMethodName() == "box")) {
            return "Fail: top stack trace element should be box() from default package, but was $top"
        }
        return "OK"
    }
}
