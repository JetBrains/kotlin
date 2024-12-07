// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

fun f1(x: String) {}
fun f2(f: () -> Unit) {}
fun test1() = f2(::<!INAPPLICABLE_CANDIDATE("fun f1(x: String): Unit")!>f1<!>)


@Target(AnnotationTarget.TYPE_PARAMETER,  AnnotationTarget.TYPE)
annotation class Ann

fun <@Ann R : @Ann Any> f3(a: Array<@Ann R>): Array<@Ann R?> =  null!!

fun test2(a: @Ann Array<in @Ann Int>) {
    val r: Array<in Int?> = f3(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<@Ann() R (of fun <R : @Ann() Any> f3)>; @Ann() kotlin.Array<CapturedType(in @Ann() kotlin.Int)>")!>a<!>)
}


var test3: Int = 0
    set(s: <!WRONG_SETTER_PARAMETER_TYPE("kotlin.Int; @Ann() kotlin.String")!>@Ann String<!>) {}


fun f4(fn: (@Ann Int, @Ann Int) -> Unit) {}

val test4 = f4 <!ARGUMENT_TYPE_MISMATCH("kotlin.Function2<@Ann() kotlin.Int, @Ann() kotlin.Int, kotlin.Unit>; kotlin.Function1<@Ann() kotlin.Int, kotlin.Unit>")!>{ single -> }<!>
