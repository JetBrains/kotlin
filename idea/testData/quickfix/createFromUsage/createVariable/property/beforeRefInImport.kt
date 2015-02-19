// "Create property 'foo'" "false"
// ERROR: Unresolved reference: foo

package p

import p.<caret>foo

fun test() {

}