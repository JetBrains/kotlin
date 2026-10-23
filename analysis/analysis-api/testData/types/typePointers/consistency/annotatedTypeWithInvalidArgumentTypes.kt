// WITH_STDLIB
class A

@Target(AnnotationTarget.TYPE)
annotation class Complex(
    val arrayOfArrays: Array<Array<Int>>,
    val classA: A,
    val classAArray: Array<A>,
    val functionType: () -> Unit,
    val intArray: IntArray,
    vararg val x: Int
)

@Target(AnnotationTarget.TYPE)
annotation class SomeTypeAnnotation

fun test(
    value: <expr>@Complex(
    arrayOfArrays = [[1, 2], [3, 4]],
    classA = A(),
    classAArray = [A(), A()],
    functionType = {},
    intArray = arrayOf(true, false),
) List<String></expr>) {
}
