// IGNORE_FIR

val <error descr="Destructuring declarations are only allowed for local variables/values">(a, b)</error> by <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">lazy</error> { <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">Pair</error>(1, 2) }
val <error descr="Destructuring declarations are only allowed for local variables/values">(c, d)</error> = <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">run</error> { <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">Pair</error>(3, 4) }


class Foo {
    val <error descr="Destructuring declarations are only allowed for local variables/values">(a, b)</error> by <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">lazy</error> { <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">Pair</error>(1, 2) }
    val <error descr="Destructuring declarations are only allowed for local variables/values">(c, d)</error> = <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">run</error> { <error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">Pair</error>(3, 4) }
}

// NO_CHECK_INFOS
// WITH_RUNTIME