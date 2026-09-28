// RUN_PIPELINE_TILL: CODEGEN
open class Foo {
    open protected fun bar(a: dynamic){
        a.something
    }
}
