// FIR_IDENTICAL
// FIR_DUMP
package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val int: Int)

interface Base
fun bar(): Base = object : Base {}

const val constant = 0

class MyClass: @Anno(constant) Base by bar() {
    @Target(AnnotationTarget.TYPE)
    annotation class Anno(val string: String)
}
