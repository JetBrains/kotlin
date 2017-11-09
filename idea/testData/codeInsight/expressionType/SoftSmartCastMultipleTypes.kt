interface A
interface B

fun fn(value: Any) {
    if (value is B) {
        if (value is A) {
            println(valu<caret>e)

        }
    }
}

// TYPE: value -> <html>A & B (smart cast from Any)</html>
