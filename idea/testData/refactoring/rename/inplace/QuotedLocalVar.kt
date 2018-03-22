class A(val a: B)
class B(val b: Int)

fun x(`is`: A) {
    val <caret>`in` = `is`.a
    `in`.b
}