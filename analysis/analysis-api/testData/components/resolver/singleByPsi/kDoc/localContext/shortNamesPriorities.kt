package AA

/** [A<caret_1>A] - to class A */
class AA
/** [A<caret_2>A] - to fun A(a: Int) */
fun AA(a: Int) {}


/** [b<caret_3>b] - to val bb */
val bb = 0
/** [b<caret_4>b] - to fun bb() */
fun bb() = 0

/**
 * [b<caret_5>b] - to fun bb()
 * since the priority of a function is higher than the prioritiy of a property
 *
 * [A<caret_6>A] - to the class A
 * since the priority of a class is higher than the prioritiy of a function
 */
 fun usage() = 0