// "Replace usages of 'typealias OldAlias = A<Int>' in whole project" "true"

package test

import dependency.d.OldAlias

fun foo(a: <caret>OldAlias): OldAlias? = null

val usage: OldAlias = OldAlias()
