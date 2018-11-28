// "Replace usages of 'OldAnnotation' in whole project" "true"

package test

import dependency.NewAnnotation

fun foo(a: NewAnnotation) {
}

@<caret>NewAnnotation
class X