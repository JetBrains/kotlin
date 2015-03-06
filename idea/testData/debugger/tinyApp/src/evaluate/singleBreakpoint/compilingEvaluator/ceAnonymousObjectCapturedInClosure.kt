package ceAnonymousObjectCapturedInClosure

fun main(args: Array<String>) {
    val localObject = object { fun test() = 1 }
    var localObjectVar = object { fun test() = 1 }
    //Breakpoint!
    lambda { localObjectVar.test() }
}

fun lambda(f: () -> Int) = f()

// EXPRESSION: lambda { localObject.test() }
// RESULT: 1: I

// EXPRESSION: lambda { localObjectVar.test() }
// RESULT: 1: I