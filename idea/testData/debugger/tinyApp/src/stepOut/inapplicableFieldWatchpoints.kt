package inapplicableFieldWatchpoints

class A {
    //FieldWatchpoint! (propWithGet)
    val propWithGet: Int get() = 1
}

interface T {
    //FieldWatchpoint! (propInInterface)
    val propInInterface: Int
}

fun main(args: Array<String>) {
    //FieldWatchpoint! (localVal)
    val localVal = 1
}

fun foo(
        //FieldWatchpoint! (funParam)
        funParam: Int
) {

}