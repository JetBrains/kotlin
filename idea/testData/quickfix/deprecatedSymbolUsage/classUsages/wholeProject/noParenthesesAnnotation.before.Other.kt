// "Replace with 'test.Bar'" "true"

package x

import dependency.*

annotation class A(val a: OldAnnotation)

@A(OldAnnotation()) class Y
