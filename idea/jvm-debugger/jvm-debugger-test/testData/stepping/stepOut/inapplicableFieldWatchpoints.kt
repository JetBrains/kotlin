package inapplicableFieldWatchpoints

fun main(args: Array<String>) {
    //FieldWatchpoint! (localVal)
    val localVal = 1
}

fun foo(
        //FieldWatchpoint! (funParam)
        funParam: Int
) {

}