// RUN_PIPELINE_TILL: BACKEND
class Controller<T2>

fun <T1> generate(
    block: (Controller<T1>) -> Unit
): T1 = TODO()

fun <E3> foobar(
    cont: Controller<E3>
) {}

fun foo() {
    generate { cont ->
        foobar(cont as Controller<String>)
        baz(cont)
    }
}

fun baz(cont: Controller<String>) {}