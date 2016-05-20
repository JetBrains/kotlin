// "Make 'x' public" "true"

open class Base(protected open val x: Int)

class First(override val x: Int) : Base(x)

class Second(f: First) {
    val y = f.<caret>x
}
