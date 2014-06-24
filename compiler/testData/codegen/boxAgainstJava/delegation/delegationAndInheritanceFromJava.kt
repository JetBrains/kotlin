import delegationAndInheritanceFromJava.*
import java.util.HashSet

class Impl(b: B): A, B by b

fun box() = "OK"
