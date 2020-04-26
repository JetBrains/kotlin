// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

package test.regressions.kt1149

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



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_ARRAY_LIST
