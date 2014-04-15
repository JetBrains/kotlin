package enums

import enums.MyEnum.A

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

enum class MyEnum { A }

// EXPRESSION: A == MyEnum.A
// RESULT: 1: Z
