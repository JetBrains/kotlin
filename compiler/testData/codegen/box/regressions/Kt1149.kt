// WITH_RUNTIME

package test.regressions.kt1149

import java.util.ArrayList

public interface SomeTrait {
    fun foo()
}

fun box(): String {
    val list = ArrayList<SomeTrait>()
    var res = ArrayList<String>()
    list.add(object : SomeTrait {
        override fun foo() {
            res.add("anonymous.foo()")
        }
    })
    list.forEach{ it.foo() }
    return if ("anonymous.foo()" == res[0]) "OK" else "fail"
}
