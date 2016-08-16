import ppp1.*
import ppp3.MyClass6
import ppp1.MyClass7 as AnotherClass7

val v = My<caret>

// INVOCATION_COUNT: 2

/* explicitly imported */
// ORDER: MyClass6

/* imported with * */
// ORDER: MyClass1

/* imported with * */
// ORDER: MyClass2

/* another class from the same package imported */
// ORDER: MyClass5

/* not imported */
// ORDER: MyClass3

/* not imported */
// ORDER: MyClass4

/* not imported */
// ORDER: MyClass7
