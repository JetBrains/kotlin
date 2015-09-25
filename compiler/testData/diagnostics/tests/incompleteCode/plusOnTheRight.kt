//EA-35646
package a

class MyClass1 {
    public operator fun plus() {}
}

fun main(arg: MyClass1) {
    arg<!TOO_MANY_ARGUMENTS!>+<!><!SYNTAX!><!>
}
