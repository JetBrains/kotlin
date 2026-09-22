// TARGET_BACKEND: JVM
// ISSUE: KT-72345
import java.util.Properties

class MyProperties : Properties()

fun box(): String {
    val p = MyProperties()
    p.setProperty("my.prop", "OK")
    return if (p.containsValue("OK")) {
        "OK"
    } else {
        "fail"
    }
}
