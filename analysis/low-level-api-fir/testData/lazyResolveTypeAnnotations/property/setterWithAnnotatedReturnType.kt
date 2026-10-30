@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class A {
    var pr<caret>operty = 1
        get() = field
        set(value): @Anno("setter return type $prop") Unit {
            field = value
        }
}
