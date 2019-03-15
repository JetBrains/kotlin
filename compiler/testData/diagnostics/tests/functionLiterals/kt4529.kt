//KT-4529 Lambdas are analyzed improperly in an infix call nested inside a println

class G {
    infix fun foo(bar: (Int) -> Int) = bar
}

fun main() {
    use(
            G().foo {it + 11} // no error
    )
    use(
            G() foo {it + 11} // ERROR
    )
    use(
            G() foo ({it + 1}) // 2 ERRORs
    )
}

fun use(a: Any?) = a