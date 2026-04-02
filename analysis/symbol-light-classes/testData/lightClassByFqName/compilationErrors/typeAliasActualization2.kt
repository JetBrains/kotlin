// common.pack.ClassToCheck
// MODULE: m1-common
// FILE: common.kt
package common.pack

annotation class RegularAnnotation
expect annotation class ExpectAnnotation
expect class ExpectClass

@RegularAnnotation
@ExpectAnnotation
class ClassToCheck {
    fun t(t: ExpectClass) {

    }
}
// MODULE: main-jvm()()(m1-common)
// FILE: annotations.kt
package jvm.pack

annotation class ActualAnnotation
class ActualClass
// FILE: jvm.kt
package common.pack

import jvm.pack.ActualAnnotation
import jvm.pack.ActualClass

actual typealias ExpectAnnotation = ActualAnnotation
actual typealias ExpectClass = ActualClass
