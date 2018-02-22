class A(val a: B)
class B(val b: Int)

fun x(<caret>`is`: A) {
    val `in` = `is`.a
    `in`.b
}