// BODY_RESOLVE
package lowlevel

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

fun reso<caret>lveMe() {
    class A {
        var implicitType
            get() = explicitType()
            set(value) {

            }

        fun explicitType(): @Anno("return type $prop") List<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>> = 0
    }
}