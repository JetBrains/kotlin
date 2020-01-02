// See KT-9145

@Target(AnnotationTarget.CLASS)
annotation class Ann

fun foo() {
    for (@Ann private x in 1..100) {
        if (x == 1) return
    }
}