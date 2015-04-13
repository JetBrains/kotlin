package pack

fun String.extFoo(){}

fun foo() {
    pack.<caret>
}

// ABSENT: extFoo
