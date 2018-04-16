/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

enum TestError : Error {
    case assertFailed(String)
    case failure
    case testsFailed([String])
}

// ---------------- Assertions ----------------

func assertEquals<T: Equatable>(actual: T, expected: T,
                                _ message: String = "Assertion failed:") throws {
    if (actual != expected) {
        throw TestError.assertFailed(message + " Expected value: \(expected), but got: \(actual)")
    }
}

func assertEquals<T: Equatable>(actual: [T], expected: [T],
                                _ message: String = "Assertion failed: arrays not equal") throws {
    try assertEquals(actual: actual.count, expected: expected.count, "Size differs")
    try assertTrue(actual.elementsEqual(expected), "Arrays elements are not equal")
}

func assertTrue(_ value: Bool, _ message: String = "Assertion failed:") throws {
    if (value != true) {
        throw TestError.assertFailed(message + " Expected value to be TRUE, but got: \(value)")
    }
}

func assertFalse(_ value: Bool, _ message: String = "Assertion failed:") throws {
    if (value != false) {
        throw TestError.assertFailed(message + " Expected value to be FALSE, but got: \(value)")
    }
}


// ---------------- Utils --------------------

func withAutorelease( _ method: @escaping () throws -> Void) -> () throws -> Void {
    return { () throws -> Void in
        try autoreleasepool { try method() }
    }
}

// ---------------- Execution ----------------

private final class Statistics : CustomStringConvertible {
    var passed: [String] = []
    var failed: [String] = []

    var description: String {
        return """
        ---- RESULTS:
         PASSED: \(passed.count)
         FAILED: \(failed.count)
        """
    }

    static let instance = Statistics()

    static func getInstance() -> Statistics {
        return instance
    }

    func start(_ name: String) {
        print("---- Starting test: \(name)")
    }

    func passed(_ name: String) {
        print("---- PASSED test: \(name)")
        passed.append(name)
    }

    func failed(_ name: String, error: Error) {
        print("---- FAILED test: \(name) with error: \(error)")
        failed.append(name)
    }
}
/**
 *  TestCase represents a single test
 */
struct TestCase {
    let name: String
    let method: () throws -> Void

    init(name: String, method: @escaping () throws -> Void) {
        self.name = name
        self.method = method
    }

    func run() {
        let stats = Statistics.getInstance()
        stats.start(name)
        do {
            try method()
            stats.passed(name)
        } catch {
            stats.failed(name, error: error)
        }
    }
}

protocol TestProvider {
    var tests: [TestCase] { get }
}

var providers: [TestProvider] = []

private func execute(tests: [TestCase]) {
    for test in tests {
        test.run()
    }
}

/**
 * Entry point of the test
 */
private func main() {
    // Generated method that instantiates TestProvider
    registerProvider()

    let stats = Statistics.getInstance()
    for pr in providers {
        execute(tests: pr.tests)
    }
    print(stats)

    let failed = stats.failed
    if !failed.isEmpty {
        print()
        print("Tests failed:")
        for testName in failed {
            print(":: \(testName)")
        }
        abort()
    }
}

main()