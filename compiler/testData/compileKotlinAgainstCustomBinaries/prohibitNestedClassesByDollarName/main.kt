import test.Outer
import test.JavaOuter

fun main(args: Array<String>) {
    Outer.Nested()
    test.`Outer$Nested`()

    JavaOuter.JavaNested()
    test.`JavaOuter$JavaNested`()
}
