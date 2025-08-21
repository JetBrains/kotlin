// COMPILATION_ERRORS
const val CONSTANT = 0

@InvalidAnno("${CONSTANT ${}}")
fun test() {
    val a = 1
    val b = "${a ${}}"
}
