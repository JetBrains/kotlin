//KT-4247 LABEL_NAME_CLASH

fun foo(bar1: (String.() -> Int) -> Int) {
    bar1 {
        this.length
    }

    bar1 {
        this@bar1.length
    }
}