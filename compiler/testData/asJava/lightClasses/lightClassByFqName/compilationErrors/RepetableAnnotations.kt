// p.Annotations

package p


class Annotations {

    @R("a") @R("b") @R("c")
    fun repeatables1() {

    }

    @R("a")
    fun repeatables2() {

    }

    @R("a") @S("b") @R("c") @S("D") @R("f")
    fun repeatables3() {

    }

}

@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class S(val g: String)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class R(val s: String)

// FIR_COMPARISON