// KT-9145

@Target(AnnotationTarget.CLASS)
annotation class Ann

var x: Int
    get() = 1
    set(@Ann private x) { }