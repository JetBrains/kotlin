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

func testNumbers() throws {
    try assertEquals(actual: ValuesBoolean(value: true).boolValue, expected: true)
    try assertEquals(actual: ValuesBoolean(value: false).intValue, expected: 0)
    try assertEquals(actual: ValuesBoolean(value: true), expected: true)
    try assertFalse(ValuesBoolean(value: false) as! Bool)

    try assertEquals(actual: ValuesByte(value: -1).int8Value, expected: -1)
    try assertEquals(actual: ValuesByte(value: -1).int32Value, expected: -1)
    try assertEquals(actual: ValuesByte(value: -1).doubleValue, expected: -1.0)
    try assertEquals(actual: ValuesByte(value: -1), expected: NSNumber(value: Int64(-1)))
    try assertFalse(ValuesByte(value: -1) == NSNumber(value: -1.5))
    try assertEquals(actual: ValuesByte(value: -1), expected: -1)
    try assertTrue(ValuesByte(value: -1) == -1)
    try assertFalse(ValuesByte(value: -1) == 1)
    try assertEquals(actual: ValuesByte(value: -1) as! Int32, expected: -1)

    try assertEquals(actual: ValuesShort(value: 111).int16Value, expected: 111)
    try assertEquals(actual: ValuesShort(value: -15) as! Int16, expected: -15)
    try assertEquals(actual: ValuesShort(value: 47), expected: 47)

    try assertEquals(actual: ValuesInt(value: 99).int32Value, expected: 99)
    try assertEquals(actual: ValuesInt(value: -1) as! Int32, expected: -1)
    try assertEquals(actual: ValuesInt(value: 72), expected: 72)

    try assertEquals(actual: ValuesLong(value: 65).int64Value, expected: 65)
    try assertEquals(actual: ValuesLong(value: 10000000000) as! Int64, expected: 10000000000)
    try assertEquals(actual: ValuesLong(value: 8), expected: 8)

    try assertEquals(actual: ValuesUByte(value: 17).uint8Value, expected: 17)
    try assertEquals(actual: ValuesUByte(value: 42) as! UInt8, expected: 42)
    try assertEquals(actual: 88, expected: ValuesUByte(value: 88))

    try assertEquals(actual: ValuesUShort(value: 40000).uint16Value, expected: 40000)
    try assertEquals(actual: ValuesUShort(value: 1) as! UInt16, expected: UInt16(1))
    try assertEquals(actual: ValuesUShort(value: 65000), expected: 65000)

    try assertEquals(actual: ValuesUInt(value: 3).uint32Value, expected: 3)
    try assertEquals(actual: ValuesUInt(value: UInt32.max) as! UInt32, expected: UInt32.max)
    try assertEquals(actual: ValuesUInt(value: 2), expected: 2)

    try assertEquals(actual: ValuesULong(value: 55).uint64Value, expected: 55)
    try assertEquals(actual: ValuesULong(value: 0) as! UInt64, expected: 0)
    try assertEquals(actual: ValuesULong(value: 7), expected: 7)

    try assertEquals(actual: ValuesFloat(value: 1.0).floatValue, expected: 1.0)
    try assertEquals(actual: ValuesFloat(value: 22.0) as! Float, expected: 22)
    try assertEquals(actual: ValuesFloat(value: 41.0), expected: 41)
    try assertEquals(actual: ValuesFloat(value: -5.5), expected: -5.5)

    try assertEquals(actual: ValuesDouble(value: 0.5).doubleValue, expected: 0.5)
    try assertEquals(actual: ValuesDouble(value: 45.0) as! Double, expected: 45)
    try assertEquals(actual: ValuesDouble(value: 89.0), expected: 89)
    try assertEquals(actual: ValuesDouble(value: -3.7), expected: -3.7)

    Values.ensureEqualBooleans(actual: ValuesBoolean(value: true), expected: true)
    Values.ensureEqualBooleans(actual: false, expected: false)

    Values.ensureEqualBytes(actual: ValuesByte(value: 42), expected: 42)
    Values.ensureEqualBytes(actual: -11, expected: -11)

    Values.ensureEqualShorts(actual: ValuesShort(value: 256), expected: 256)
    Values.ensureEqualShorts(actual: -1, expected: -1)

    Values.ensureEqualInts(actual: ValuesInt(value: 100000), expected: 100000)
    Values.ensureEqualInts(actual: -7, expected: -7)

    Values.ensureEqualLongs(actual: ValuesLong(value: Int64.max), expected: Int64.max)
    Values.ensureEqualLongs(actual: 17, expected: 17)

    Values.ensureEqualUBytes(actual: ValuesUByte(value: 6), expected: 6)
    Values.ensureEqualUBytes(actual: 255, expected: 255)

    Values.ensureEqualUShorts(actual: ValuesUShort(value: 300), expected: 300)
    Values.ensureEqualUShorts(actual: 65535, expected: UInt16.max)

    Values.ensureEqualUInts(actual: ValuesUInt(value: 70000), expected: 70000)
    Values.ensureEqualUInts(actual: 48, expected: 48)

    Values.ensureEqualULongs(actual: ValuesULong(value: UInt64.max), expected: UInt64.max)
    Values.ensureEqualULongs(actual: 39, expected: 39)

    Values.ensureEqualFloats(actual: ValuesFloat(value: 36.6), expected: 36.6)
    Values.ensureEqualFloats(actual: 49.5, expected: 49.5)
    Values.ensureEqualFloats(actual: 18, expected: 18.0)

    Values.ensureEqualDoubles(actual: ValuesDouble(value: 12.34), expected: 12.34)
    Values.ensureEqualDoubles(actual: 56.78, expected: 56.78)
    Values.ensureEqualDoubles(actual: 3, expected: 3)

    func checkBox<T: Equatable, B : NSObject>(_ value: T, _ boxFunction: (T) -> B?) throws {
        let box = boxFunction(value)!
        try assertEquals(actual: box as! T, expected: value)
        print(type(of: box))
        print(B.self)
        try assertTrue(box.isKind(of: B.self))
    }

    try checkBox(true, Values.box)
    try checkBox(Int8(-1), Values.box)
    try checkBox(Int16(-2), Values.box)
    try checkBox(Int32(-3), Values.box)
    try checkBox(Int64(-4), Values.box)
    try checkBox(UInt8(5), Values.box)
    try checkBox(UInt16(6), Values.box)
    try checkBox(UInt32(7), Values.box)
    try checkBox(UInt64(8), Values.box)
    try checkBox(Float(8.7), Values.box)
    try checkBox(Double(9.4), Values.box)
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

func testInlineClasses() throws {
    let ic1: Int32 = 42
    let ic1N = Values.box(ic1: 17)
    let ic2 = "foo"
    let ic2N = "bar"
    let ic3 = ValuesTripleVals(first: 1, second: 2, third: 3)
    let ic3N = Values.box(ic3: nil)

    try assertEquals(
        actual: Values.concatenateInlineClassValues(ic1: ic1, ic1N: ic1N, ic2: ic2, ic2N: ic2N, ic3: ic3, ic3N: ic3N),
        expected: "42 17 foo bar TripleVals(first=1, second=2, third=3) null"
    )

    try assertEquals(
        actual: Values.concatenateInlineClassValues(ic1: ic1, ic1N: nil, ic2: ic2, ic2N: nil, ic3: nil, ic3N: nil),
        expected: "42 null foo null null null"
    )
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
            TestCase(name: "TestNumbers", method: withAutorelease(testNumbers)),
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
            TestCase(name: "TestInlineClasses", method: withAutorelease(testInlineClasses)),
        ]
    }
}
