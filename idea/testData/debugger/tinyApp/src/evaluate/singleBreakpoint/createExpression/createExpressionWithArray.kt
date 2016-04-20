package createExpressionWithArray

import forTests.MyJavaClass
// do not remove this import, it checks that we do not insert ambiguous imports during EE
import forTests.MyJavaClass.InnerClass

fun main(args: Array<String>) {
    val baseArray = arrayOf(MyJavaClass().getBaseClassValue())
    val innerArray = arrayOf(MyJavaClass().getInnerClassValue())
    //Breakpoint!
    val a = 1
}

// PRINT_FRAME
// DESCRIPTOR_VIEW_OPTIONS: NAME_EXPRESSION_RESULT
