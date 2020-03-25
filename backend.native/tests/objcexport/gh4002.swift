import Kt

// See https://github.com/JetBrains/kotlin-native/issues/4002

class GH4002Base0 : NSObject, NSCoding {
  required init(coder: NSCoder) { fatalError() }

  func encode(with coder: NSCoder) { fatalError() }
}

class GH4002Base1<T : GH4002ArgumentBase> : GH4002Base0 {}

@objc(ObjCGH4002)
class GH4002 : GH4002Base1<GH4002Argument> {}

private func test1() throws {
    try assertEquals(actual: String(cString: class_getName(GH4002.self)), expected: "ObjCGH4002")
}

class Gh4002Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}