// !RENDER_DIAGNOSTICS_MESSAGES
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER
// !WITH_NEW_INFERENCE

fun f1(x: String) {}
fun f2(f: () -> Unit) {}
fun test1() = <!INAPPLICABLE_CANDIDATE!>f2<!>(::<!UNRESOLVED_REFERENCE!>f1<!>)


@Target(AnnotationTarget.TYPE_PARAMETER,  AnnotationTarget.TYPE)
annotation class Ann

fun <@Ann R : @Ann Any> f3(a: Array<@Ann R>): Array<@Ann R?> =  null!!

fun test2(a: @Ann Array<in @Ann Int>) {
    val r: Array<in Int?> = f3(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}


var test3: Int = 0
    set(s: <!WRONG_SETTER_PARAMETER_TYPE!>@Ann String<!>) {}


fun f4(fn: (@Ann Int, @Ann Int) -> Unit) {}

val test4 = f4 <!ARGUMENT_TYPE_MISMATCH!>{ single -> }<!>
