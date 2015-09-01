// "Replace usages of 'OldAnnotation' in whole project" "true"

package test

import dependency.NewAnnotation

fun foo(a: NewAnnotation) {
}

@NewAnnotation(1) class X