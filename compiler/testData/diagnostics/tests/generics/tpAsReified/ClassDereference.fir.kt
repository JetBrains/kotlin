
fun <T: Any> dereferenceClass(): Any =
        <!OTHER_ERROR!>T<!>::class
