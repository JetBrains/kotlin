// "Replace usages of 'OldClass' in whole project" "true"

import pack.OldClass

class B : OldClass<caret>(42)

class C : OldClass(42)

fun foo() {
    val b = 42
    OldClass(b)
}

