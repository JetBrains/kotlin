class B {
    default object <error>A</error> {
    }

    val <error>A</error> = this
}

class C {
    default <error>object A</error> {
        <error>val A</error> = this
    }

}

class D {
    default <error>object A</error> {
        <error>val `OBJECT$`</error> = this
    }

    val `OBJECT$` = D
}