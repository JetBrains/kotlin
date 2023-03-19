// Char issues
// IGNORE_BACKEND: JS_IR

// MODULE: main
// FILE: externals.js

function apply7(f) {
    return f("a")("b")("c")("d")("e")("f")("g")
}

function extenalWithLambda(x, dc) {
    x(true, 1, 2, '3'.charCodeAt(0), 4, 5n, 6.1, 7.1, "S", create123Array(), dc);
}

function create123Array() { return [1, 2, 3]; }

function is123Array(ei) {
    return ei[0] === 1 && ei[1] === 2 && ei[2] === 3;
}

function externalWithLambdas2(
boolean, // () -> Boolean,
byte, // () -> Byte,
short, // () -> Short,
char, // () -> Char,
int, // () -> Int,
long, // () -> Long,
float, // () -> Float,
double, // () -> Double,
string, // () -> String,
ei, // () -> EI,
dc, // () -> DC,
dcGetY, // (DC) -> Int
) {
    let result = 0
    function test(boolean) {
        if (boolean !== true) throw "Fail" + result
        result++
    }
    test(boolean() === true)
    test(byte() === 100)
    test(short() === 200)
    test(char() === "я".charCodeAt(0))
    test(int() === 300)
    test(long() === 400n)
    test(float() === 500.5)
    test(double() === 600.5)
    test(string() === "700")
    test(is123Array(ei()))
    test(dcGetY(dc()) == 800)
    return result
}

function createJsLambda() {
    return (
            boolean,
    byte,
    short,
    char,
    int,
    long,
    float,
    double,
    string,
    ei,
    dc,
    dcGetY
    ) => {
        let result = 0;
        function test(x) {
            if (x !== true) throw "Fail" + result;
            result++;
        }
        test(boolean === true);
        test(byte === 100);
        test(short === 200);
        test(char === "я".charCodeAt(0));
        test(int === 300);
        test(long === 400n);
        test(float === 500.5);
        test(double === 600.5);
        test(string === "700");
        test(is123Array(ei));
        test(dcGetY(dc) == 800);
        return result
    };
}

// FILE: externals.kt

external fun createJsLambda(): (Boolean, Byte, Short, Char, Int, Long, Float, Double, String, EI, JSDC, (JSDC) -> Int) -> Int

external fun apply7(f: (String) -> ((String) -> ((String) -> ((String) -> ((String) -> ((String) -> ((String) -> String))))))): String

external interface EI

external fun is123Array(x: EI): Boolean
external fun create123Array(): EI

data class DC(val x: Int, val y: Int)
typealias JSDC = JsHandle<DC>

external fun extenalWithLambda(
    x: (Boolean, Byte, Short, Char, Int, Long, Float, Double, String, EI, JSDC) -> Unit,
    dc: JSDC,
)

external fun externalWithLambdas2(
    boolean: () -> Boolean,
    byte: () -> Byte,
    short: () -> Short,
    char: () -> Char,
    int: () -> Int,
    long: () -> Long,
    float: () -> Float,
    double: () -> Double,
    string: () -> String,
    ei: () -> EI,
    dc: () -> JSDC,
    dcGetY: (JSDC) -> Int,
): Int

@JsExport
fun exportedF() : (Int, Int, Int) -> (String, String) -> String =
    { i1: Int, i2: Int, i3: Int ->
        { s1: String, s2: String ->
            "$s1${i1 + i2 + i3}$s2"
        }
    }


typealias SS2S = (String, String) -> String

@JsExport
fun complexHigherOrder(x: (SS2S, SS2S) -> SS2S): (SS2S, SS2S) -> SS2S =
    { ss2s1: SS2S, ss2s2: SS2S -> x(ss2s1, ss2s2) }

fun complexHigherOrerTest() {
    val x = { ss2s1: SS2S, ss2s2: SS2S ->
        { s1: String, s2: String -> ss2s1(s1, s2) + "_" + ss2s2(s1, s2) }
    }
    val y = complexHigherOrder(x)
    val res = y({s1, s2 -> s1 + "+" + s2}, {s1, s2 -> s1 + "-" + s2})("abc", "xyz")
    if (res != "abc+xyz_abc-xyz") {
        error("Fail complexHigherOrderTest $res")
    }
}


fun box(): String {
    val res = apply7 { s1 -> { s2 -> { s3 -> { s4 -> { s5 -> { s6 -> { s7 -> s1 + s2 + s3 + s4 + s5 + s6 + s7 } } } } } } }
    if (res != "abcdefg") return "Fail"

    var extenalWithLambdasCount = 0
    fun test(x: Boolean) {
        if (!x) error("Fail")
        extenalWithLambdasCount++
    }
    extenalWithLambda({ bool: Boolean,
                        byte: Byte,
                        short: Short,
                        char: Char,
                        int: Int,
                        long: Long,
                        float: Float,
                        double: Double,
                        string: String,
                        ei: EI,
                        jsdc: JSDC ->
            val dc = jsdc.get()
            test(bool == true)
            test(byte == 1.toByte())
            test(short == 2.toShort())
            test(char == '3')
            test(int == 4)
            test(long == 5L)
            test(float == 6.1f)
            test(double == 7.1)
            test(string == "S")
            test(is123Array(ei))
            test(dc.x == 100 && dc.y == 200)
         }, DC(100, 200).toJsHandle())


    if (extenalWithLambdasCount != 11) return "Fail 1"

    val externalWithLambdas2Count = externalWithLambdas2(
        boolean = { true },
        byte = { 100.toByte() },
        short = { 200.toShort() },
        char = { 'я' },
        int = { 300 },
        long = { 400L },
        float = { 500.5f },
        double = { 600.5 },
        string = { "700" },
        ei = { create123Array() },
        dc = { DC(800, 800).toJsHandle() },
        dcGetY = { it.get().y }
    )
    if (externalWithLambdas2Count != 11) return "Fail externalWithLambdas2"

    val externalWithLambdas2Ref = ::externalWithLambdas2
    val externalWithLambdas2RefCount = externalWithLambdas2Ref.invoke(
        { true },
        { 100.toByte() },
        { 200.toShort() },
        { 'я' },
        { 300 },
        { 400L },
        { 500.5f },
        { 600.5 },
        { "700" },
        { create123Array() },
        { DC(800, 800).toJsHandle() },
        { it.get().y }
    )
    if (externalWithLambdas2RefCount != 11) return "Fail externalWithLambdas2"

    val createJsLambdaRef = ::createJsLambda
    for (jsLambda in arrayOf(createJsLambda(), createJsLambdaRef.invoke())) {
        val jsLambdaCount = jsLambda(
            true,
            100.toByte(),
            200.toShort(),
            'я',
            300,
            400L,
            500.5f,
            600.5,
            "700",
            create123Array(),
            DC(800, 800).toJsHandle(),
            { it.get().y }
        )
        if (jsLambdaCount != 11)
            return "Fail 3"
    }

    complexHigherOrerTest()

    return "OK"
}

// FILE: entry.mjs

import main from "./index.mjs"

const boxResult = main.box();

if (boxResult != "OK") {
    throw `Wrong box result '${boxResult}'; Expected "OK"`;
}

const exportedFres = main.exportedF()(1, 20, 300)("<", ">");
if (exportedFres !== "<321>")
    throw "exportedF fail " + exportedFres;

(function testComplexHighOrder() {
    const x = (ss2s1, ss2s2) => (s1, s2) => ss2s1(s1, s2) + "_" + ss2s2(s1, s2);

    const y = main.complexHigherOrder(x);
    const res = y((s1, s2) => s1 + "+" + s2, (s1, s2) => s1 + "-" + s2)("abc", "xyz");
    if (res !== "abc+xyz_abc-xyz") {
        throw "Fail complexHigherOrderTest " + res;
    }
})();
