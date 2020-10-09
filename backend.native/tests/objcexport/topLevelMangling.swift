import Kt

private func testFunctionsInDifferentFilesAreNotMangled() throws {
    try assertEquals(actual: TopLevelManglingAKt.foo(), expected: "a1")
    try assertEquals(actual: TopLevelManglingBKt.foo(), expected: "b1")
}

private func testPropertiesInDifferentFilesAreNotMangled() throws {
    try assertEquals(actual: TopLevelManglingAKt.bar, expected: "a2")
    try assertEquals(actual: TopLevelManglingBKt.bar, expected: "b2")
}

class TopLevelManglingTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestFunctionsInDifferentFilesAreNotMangled", testFunctionsInDifferentFilesAreNotMangled)
        test("TestPropertiesInDifferentFilesAreNotMangled", testPropertiesInDifferentFilesAreNotMangled)
    }
}
