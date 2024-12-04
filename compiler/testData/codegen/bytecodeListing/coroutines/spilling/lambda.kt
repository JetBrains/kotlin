// WITH_STDLIB

fun use2(c: suspend Long.(Double, String) -> Unit) {}
fun use(c: suspend Long.(String) -> Unit) {}

fun blackhole(a: Any) {}

fun test() {
    // test$1: p$0, p$1
    use2 { a, b ->
        blackhole(b + a)
    }
    // test$2: p$0
    use2 { a, b ->
        blackhole(a)
    }
    // test$3: p$1
    use2 { a, b ->
        blackhole(b)
    }
    // test$4: p$0
    use2 { a, _ ->
        blackhole(a)
    }
    // test$5: p$1
    use2 { _, b ->
        blackhole(b)
    }
    // test$6: p$, p$0, p$1
    use2 { a, b ->
        blackhole(b + a + this)
    }
    // test$7: p$, p$0
    use2 { a, b ->
        blackhole(a + this.toDouble())
    }
    // test$8: p$, p$1
    use2 { a, b ->
        blackhole(b + this)
    }
    // test$9: p$, p$0
    use2 { a, _ ->
        blackhole(a + this.toDouble())
    }
    // test$10: p$, p$1
    use2 { _, b ->
        blackhole(b + this)
    }
    // test$11: p$
    use2 { _, _ ->
        blackhole(this)
    }
    // test$12:
    use2 { _, _ -> }
    // test$13: p$, p$0
    use {
        blackhole(it + this)
    }
    // test$14: p$
    use {
        blackhole(this)
    }
    // test$15: p$0
    use {
        blackhole(it)
    }
    // test$16:
    use {}
    // test$17: p$
    use {
        blackhole(toString())
    }
}
