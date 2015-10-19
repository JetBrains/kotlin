class A0
constructor() {}
class A1
private constructor(y: Int) : Base1(), Base2 {
    val x: Int
}
class A2 @private constructor(y: Int)

class A3 @Ann(1) private constructor(y: Int)

class A4 private @Ann(1) constructor(y: Int)

class A5 @Ann private constructor() {}

class A6 @Ann() private constructor() {}
