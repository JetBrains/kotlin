// !WITH_NEW_INFERENCE
class C<reified T>

fun <T> id(p: T): T = p

fun <A> main() {
    C()

    val a: C<A> = C()
    C<A>()

    val b: C<Int> = C()
    C<Int>()

    // TODO svtk, uncomment when extensions are called for nested calls!
    //val < !UNUSED_VARIABLE!>с< !>: C<A> = id(< !TYPE_PARAMETER_AS_REIFIED!>C< !>())
}