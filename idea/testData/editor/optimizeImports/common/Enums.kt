package foo

import dependency.D
import dependency.D.E1
import dependency.D.E1.E11
import dependency.D.E1.E12
import dependency.D.E2
import dependency.D.E2.E21
import dependency.D.E2.E22
import dependency.D.E3
import dependency.D.E3.E31
import dependency.D.E3.E32

fun foo {
    E11
    D.E2.E22
    E3.E31
}