//KT-610 Distinguish errors 'unused variable' and 'variable is assigned but never accessed'

package kt610

fun foo() {
    var j = 9  //'unused variable' error

    var i = 1  //should be an error 'variable i is assigned but never accessed'
    i = 2
}
