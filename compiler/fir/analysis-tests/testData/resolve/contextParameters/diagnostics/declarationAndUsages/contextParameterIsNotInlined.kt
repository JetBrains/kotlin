// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

fun noInlineRun(f: (A) -> Unit) { f(A()) }

context(a: (A) -> Unit)
<!NOTHING_TO_INLINE!>inline<!> fun test1(){
    noInlineRun(a)
}

context(a: (A) -> Unit)
var foo: String
    inline get() {
        noInlineRun(a)
        return ""
    }
    inline set(value) {
        noInlineRun(a)
    }

context(a: (A) -> Unit)
inline var bar: String
    get() {
        noInlineRun(a)
        return ""
    }
    set(v) {
        noInlineRun(a)
    }

fun usage() {
    with({a: A ->  }){
        test1()
        foo
        bar
    }
}
