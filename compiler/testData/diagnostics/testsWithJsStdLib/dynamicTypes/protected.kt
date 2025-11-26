// RUN_PIPELINE_TILL: BACKEND
open class Foo {
    open protected fun bar(a: dynamic){
        a.something
    }
}