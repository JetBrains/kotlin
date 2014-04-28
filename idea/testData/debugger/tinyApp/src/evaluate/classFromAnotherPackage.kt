package classFromAnotherPackage

import stepInto.MyJavaClass

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: MyJavaClass()
// RESULT: instance of stepInto.MyJavaClass(id=ID): LstepInto/MyJavaClass;

// EXPRESSION: stepInto.MyJavaClass()
// RESULT: instance of stepInto.MyJavaClass(id=ID): LstepInto/MyJavaClass;
