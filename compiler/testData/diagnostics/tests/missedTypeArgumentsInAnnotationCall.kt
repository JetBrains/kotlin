// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
package usage

annotation class B<T>

@<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>
class A
