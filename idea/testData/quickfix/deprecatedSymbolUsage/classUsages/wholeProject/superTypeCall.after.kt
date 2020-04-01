// "Replace usages of 'OldClass' in whole project" "true"

import newPack.NewClass

class B : NewClass({ 42 })

class C : NewClass({ 42 })

fun foo() {
    val b = 42
    NewClass({ b })
}

