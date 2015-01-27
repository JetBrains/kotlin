import kotlin.properties.Delegates
import java.util.HashMap

trait R {
    fun result(): String
}

val a by Delegates.lazy {
    with(HashMap<String, R>()) {
        put("result", object : R {
            override fun result(): String = "OK"
        })
        this
    }
}

fun box(): String {
    val r = a["result"]

    // Check that reflection won't fail
    r.javaClass.getEnclosingMethod().toString()

    return r.result()
}
