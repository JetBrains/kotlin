import Kt

private func test1() throws {
    try assertEquals(actual: Kt39206Kt.myFunc(), expected: 17)
}

class Kt39206Tests : SimpleTestProvider {
    override init() {
        super.init()

        test("Test1", test1)
    }
}