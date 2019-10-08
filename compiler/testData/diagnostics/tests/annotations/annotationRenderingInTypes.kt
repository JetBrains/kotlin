// !RENDER_DIAGNOSTICS_MESSAGES
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

fun f1(x: String) {}
fun f2(f: () -> Unit) {}
fun test1() = f2(<!TYPE_MISMATCH("() -> Unit", "KFunction1<String, Unit>")!>::f1<!>)


@Target(AnnotationTarget.TYPE_PARAMETER,  AnnotationTarget.TYPE)
annotation class Ann

fun <@Ann R : @Ann Any> f3(a: Array<@Ann R>): Array<@Ann R?> =  null!!

fun test2(a: @Ann Array<in @Ann Int>) {
    val r: Array<in Int?> = <!TYPE_INFERENCE_CANNOT_CAPTURE_TYPES("'R' cannot capture 'in Int'. Type parameter has an upper bound 'Any' that cannot be satisfied capturing 'in' projection")!>f3<!>(a)
}


var test3: Int = 0
    set(s: <!WRONG_SETTER_PARAMETER_TYPE("Int", "String")!>@Ann String<!>) {}


fun f4(fn: (@Ann Int, @Ann Int) -> Unit) {}

val test4 = f4 { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH("2", "Int, Int")!>single<!> -> }
