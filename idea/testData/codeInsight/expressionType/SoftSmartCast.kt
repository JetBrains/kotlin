interface A
interface B : A

fun fn(value: Any) {
    if (value is B) {
        if (<caret>value is A) { // here

        }
    }
}

// TYPE: value -> <html>B (smart cast from Any)</html>
// TYPE: value is A -> <html>Boolean</html>
