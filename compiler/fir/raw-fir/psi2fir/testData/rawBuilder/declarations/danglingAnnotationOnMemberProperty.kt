// INCONSISTENT_DECLARATIONS
// ^KT-64901
annotation class Ann

class C {
    @Ann(
    val x: Int = 42
}