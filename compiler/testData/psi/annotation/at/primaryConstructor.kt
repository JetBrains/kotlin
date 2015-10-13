class A1 @Ann1("") ()

class A2 @Ann2("")(x: Int) : B {
}

class A3 @[Ann3] private @(x: Int)
class A4 @[Ann4] @private @(x: Int)
class A6 @[Ann5] @private @ @[Ann6]()
