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
import Values

// -------- Tests --------

func testVals() throws {
    print("Values from Swift")
    let dbl = Values.dbl
    let flt = Values.flt
    let int = Values.integer
    let long = Values.longInt
    
    print(dbl)
    print(flt)
    print(int)
    print(long)
    
    try assertEquals(actual: dbl, expected: 3.14 as Double, "Double value isn't equal.")
    try assertEquals(actual: flt, expected: 2.73 as Float, "Float value isn't equal.")
    try assertEquals(actual: int, expected: 42)
    try assertEquals(actual: long, expected: 1984)
}

func testVars() throws {
    print("Variables from Swift")
    var intVar = Values.intVar
    var strVar = Values.str
    var strAsId = Values.strAsAny
    
    print(intVar)
    print(strVar)
    print(strAsId)
    
    try assertEquals(actual: intVar, expected: 451)
    try assertEquals(actual: strVar, expected: "Kotlin String")
    try assertEquals(actual: strAsId as! String, expected: "Kotlin String as Any")
    
    strAsId = "Swift str"
    Values.strAsAny = strAsId
    print(Values.strAsAny)
    try assertEquals(actual: Values.strAsAny as! String, expected: strAsId as! String)
    
    // property with custom getter/setter backed by the Kotlin's var
    var intProp : Int32 {
        get {
            return Values.intVar * 2
        }
        set(value) {
            Values.intVar = 123 + value
        }
    }
    intProp += 10
    print(intProp)
    print(Values.intVar)
    try assertEquals(actual: Values.intVar * 2, expected: intProp, "Property backed by var")
}

func testDoubles() throws {
    print("Doubles in Swift")
    let minDouble = Values.minDoubleVal as! Double
    let maxDouble = Values.maxDoubleVal as! NSNumber

    print(minDouble)
    print(maxDouble)
    print(Values.nanDoubleVal)
    print(Values.nanFloatVal)
    print(Values.infDoubleVal)
    print(Values.infFloatVal)

    try assertEquals(actual: minDouble, expected: Double.leastNonzeroMagnitude, "Min double")
    try assertEquals(actual: maxDouble, expected: Double.greatestFiniteMagnitude as NSNumber, "Max double")
    try assertTrue(Values.nanDoubleVal.isNaN, "NaN double")
    try assertTrue(Values.nanFloatVal.isNaN, "NaN float")
    try assertEquals(actual: Values.infDoubleVal, expected: Double.infinity, "Inf double")
    try assertEquals(actual: Values.infFloatVal, expected: -Float.infinity, "-Inf float")
}

func testLists() throws {
    let numbersList = Values.numbersList
    let gold = [1, 2, 13]
    for i in 0..<gold.count {
        try assertEquals(actual: gold[i], expected: Int(numbersList[i] as! NSNumber), "Numbers list")
    }

    let anyList = Values.anyList
    for i in anyList {
        print(i)
    }
//    try assertEquals(actual: gold, expected: anyList, "Numbers list")
}

func testLazyVal() throws {
    let lazyVal = Values.lazyVal
    print(lazyVal)
    try assertEquals(actual: lazyVal, expected: "Lazily initialized string", "lazy value")
}

let goldenArray = ["Delegated", "global", "array", "property"]

func testDelegatedProp() throws {
    let delegatedGlobalArray = Values.delegatedGlobalArray
    guard Int(delegatedGlobalArray.size) == goldenArray.count else {
        throw TestError.assertFailed("Size differs")
    }
    for i in 0..<delegatedGlobalArray.size {
        print(delegatedGlobalArray.get(index: i)!)
    }
}

func testGetterDelegate() throws {
    let delegatedList = Values.delegatedList
    guard delegatedList.count == goldenArray.count else {
        throw TestError.assertFailed("Size differs")
    }
    for val in delegatedList {
        print(val)
    }
}

func testNulls() throws {
    let nilVal : Any? = Values.nullVal
    try assertTrue(nilVal == nil, "Null value")

    Values.nullVar = nil
    var nilVar : Any? = Values.nullVar
    try assertTrue(nilVar == nil, "Null variable")
}

func testAnyVar() throws {
    let anyVar : Any = Values.anyValue
    print(anyVar)
    if let str = anyVar as? String {
        print(str)
        try assertEquals(actual: str, expected: "Str")
    } else {
        throw TestError.assertFailed("Incorrect type passed from Any")
    }
}

func testFunctions() throws {
    let _: Any? = Values.emptyFun()

    let str = Values.strFun()
    try assertEquals(actual: str, expected: "fooStr")

    try assertEquals(actual: Values.argsFun(i: 10, l: 20, d: 3.5, s: "res") as! String,
            expected: "res10203.5")
}

func testFuncType() throws {
    let s = "str"
    let fFunc: () -> String = { return s }
    try assertEquals(actual: Values.funArgument(foo: fFunc), expected: s, "Using function type arguments failed")
}

func testGenericsFoo() throws {
    let fun = { (i: Int) -> String in return "S \(i)" }
    // wrap lambda to workaround issue with type conversion inability:
    // (Int) -> String can't be cast to (Any?) -> Any?
    let wrapper = { (t: Any?) -> Any? in return fun(t as! Int) }
    let res = Values.genericFoo(t: 42, foo: wrapper)
    try assertEquals(actual: res as! String, expected: "S 42")
}

func testVararg() throws {
    let ktArray = ValuesStdlibArray(size: 3, init: { (_) -> Int in return 42 })
    let arr: [Int] = Values.varargToList(args: ktArray) as! [Int]
    try assertEquals(actual: arr, expected: [42, 42, 42])
}

func testStrExtFun() throws {
    try assertEquals(actual: Values.subExt("String", i: 2), expected: "r")
    try assertEquals(actual: Values.subExt("A", i: 2), expected: "nothing")
}

func testAnyToString() throws {
    try assertEquals(actual: Values.toString(nil), expected: "null")
    try assertEquals(actual: Values.toString(42), expected: "42")
}

func testAnyPrint() throws {
    print("BEGIN")
    Values.print(nil)
    Values.print("Print")
    Values.print(123456789)
    Values.print(3.14)
    Values.print([3, 2, 1])
    print("END")
}

func testLambda() throws {
    try assertEquals(actual: Values.sumLambda(3, 4), expected: 7)
}

// -------- Tests for classes and interfaces -------
class ValIEmptyExt : ValuesI {
    func iFun() -> String {
        return "ValIEmptyExt::iFun"
    }
}

class ValIExt : ValuesI {
    func iFun() -> String {
        return "ValIExt::iFun"
    }
}

func testInterfaceExtension() throws {
    try assertEquals(actual: ValIEmptyExt().iFun(), expected: "ValIEmptyExt::iFun")
    try assertEquals(actual: ValIExt().iFun(), expected: "ValIExt::iFun")
}

func testClassInstances() throws {
    try assertEquals(actual: ValuesOpenClassI().iFun(), expected: "OpenClassI::iFun")
    try assertEquals(actual: ValuesDefaultInterfaceExt().iFun(), expected: "I::iFun")
    try assertEquals(actual: ValuesFinalClassExtOpen().iFun(), expected: "FinalClassExtOpen::iFun")
    try assertEquals(actual: ValuesMultiExtClass().iFun(), expected: "PI::iFun")
    try assertEquals(actual: ValuesMultiExtClass().piFun() as! Int, expected: 42)
    try assertEquals(actual: ValuesConstrClass(i: 1, s: "str", a: "Any").iFun(), expected: "OpenClassI::iFun")
    try assertEquals(actual: ValuesExtConstrClass(i: 123).iFun(), expected: "ExtConstrClass::iFun::123-String-AnyS")
}

func testEnum() throws {
    try assertEquals(actual: Values.passEnum().enumValue, expected: 42)
    try assertEquals(actual: Values.passEnum().name, expected: "ANSWER")
    Values.receiveEnum(e: 1)
}

func testDataClass() throws {
    let f = "1"
    let s = "2"
    let t = "3"

    let tripleVal = ValuesTripleVals(first: f, second: s, third: t)
    try assertEquals(actual: tripleVal.first as! String, expected: f, "Data class' value")
    try assertEquals(actual: tripleVal.component2() as! String, expected: s, "Data class' component")
    print(tripleVal)
    try assertEquals(actual: String(describing: tripleVal), expected: "TripleVals(first=\(f), second=\(s), third=\(t))")

    let tripleVar = ValuesTripleVars(first: f, second: s, third: t)
    try assertEquals(actual: tripleVar.first as! String, expected: f, "Data class' value")
    try assertEquals(actual: tripleVar.component2() as! String, expected: s, "Data class' component")
    print(tripleVar)
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(f), \(s), \(t)]")

    tripleVar.first = t
    tripleVar.second = f
    tripleVar.third = s
    try assertEquals(actual: tripleVar.component2() as! String, expected: f, "Data class' component")
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(t), \(f), \(s)]")
}

func testCompanionObj() throws {
    try assertEquals(actual: ValuesWithCompanionAndObjectCompanion().str, expected: "String")
    try assertEquals(actual: Values.getCompanionObject().str, expected: "String")

    let namedFromCompanion = Values.getCompanionObject().named
    let named = Values.getNamedObject()
    try assertTrue(named === namedFromCompanion, "Should be the same Named object")

    try assertEquals(actual: Values.getNamedObjectInterface().iFun(), expected: named.iFun(), "Named object's method")
}

// -------- Execution of the test --------

class ValuesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "TestValues", method: withAutorelease(testVals)),
            TestCase(name: "TestVars", method: withAutorelease(testVars)),
            TestCase(name: "TestDoubles", method: withAutorelease(testDoubles)),
            TestCase(name: "TestLists", method: withAutorelease(testLists)),
            TestCase(name: "TestLazyValues", method: withAutorelease(testLazyVal)),
            TestCase(name: "TestDelegatedProperties", method: withAutorelease(testDelegatedProp)),
            TestCase(name: "TestGetterDelegate", method: withAutorelease(testGetterDelegate)),
            TestCase(name: "TestNulls", method: withAutorelease(testNulls)),
            TestCase(name: "TestAnyVar", method: withAutorelease(testAnyVar)),
            TestCase(name: "TestFunctions", method: withAutorelease(testFunctions)),
            TestCase(name: "TestFuncType", method: withAutorelease(testFuncType)),
            TestCase(name: "TestGenericsFoo", method: withAutorelease(testGenericsFoo)),
            TestCase(name: "TestVararg", method: withAutorelease(testVararg)),
            TestCase(name: "TestStringExtension", method: withAutorelease(testStrExtFun)),
            TestCase(name: "TestAnyToString", method: withAutorelease(testAnyToString)),
            TestCase(name: "TestAnyPrint", method: withAutorelease(testAnyPrint)),
            TestCase(name: "TestLambda", method: withAutorelease(testLambda)),
            TestCase(name: "TestInterfaceExtension", method: withAutorelease(testInterfaceExtension)),
            TestCase(name: "TestClassInstances", method: withAutorelease(testClassInstances)),
            TestCase(name: "TestEnum", method: withAutorelease(testEnum)),
            TestCase(name: "TestDataClass", method: withAutorelease(testDataClass)),
            TestCase(name: "TestCompanionObj", method: withAutorelease(testCompanionObj)),
        ]
    }
}
