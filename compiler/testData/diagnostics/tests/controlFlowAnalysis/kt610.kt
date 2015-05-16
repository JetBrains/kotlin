//KT-610 Distinguish errors 'unused variable' and 'variable is assigned but never accessed'

package kt610

fun foo() {
    var <!UNUSED_VARIABLE!>j<!> = 9  //'unused variable' error

    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>i<!> = 1  //should be an error 'variable i is assigned but never accessed'
    <!UNUSED_VALUE!>i =<!> 2
}
