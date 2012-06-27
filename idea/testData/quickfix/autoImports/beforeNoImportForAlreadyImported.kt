// "Import Class" "false"
// ERROR: Unresolved reference: someFun
// ERROR: Unresolved reference: test

package Teting

import Teting.test.someFun

fun main(args : Array<String>) {
    <caret>someFun
}