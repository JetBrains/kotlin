// "Replace with 'test.Bar'" "true"

package x

import dependency.*

annotation class A(val a: NewAnnotation)

@A(NewAnnotation()) class Y
