// FILE: externals.js

// -- Strings --

function isTestString(x) {
    return x === "Test string";
}

function getTestString() {
    return "Test string";
}

function concatStrings(x, y) {
    if (typeof x !== "string") return "Fail 1";
    if (typeof y !== "string") return "Fail 2";
    return x + y;
}

function concatStringsNullable(x, y) {
    return concatStrings(x ?? "<null>", y ?? "<null>")
}

// -- Booleans --

function isTrueBoolean(x) {
    if (typeof x !== "boolean") return "Fail";
    return x === true;
}

function isFalseBoolean(x) {
    if (typeof x !== "boolean") return "Fail";
    return x === false;
}

function getTrueBoolean(x) {
    return true;
}

function getFalseBoolean(x) {
    return false;
}

// -- Any --

function createJsObjectAsAny() {
    return { value: "object created by createJsObjectAsAny" };
}

function createJsObjectAsExternalInterface() {
    return { value: "object created by createJsObjectAsExternalInterface" };
}

function getObjectValueEI(x) {
    return x.value;
}

function getObjectValueAny(x) {
    return x.value;
}

// FILE: externals.kt

external fun isTestString(x: String): Boolean
external fun getTestString(): String
external fun concatStrings(x: String, y: String): String
external fun concatStringsNullable(x: String?, y: String?): String?

external fun isTrueBoolean(x: Boolean): Boolean
external fun isFalseBoolean(x: Boolean): Boolean
external fun getTrueBoolean(): Boolean
external fun getFalseBoolean(): Boolean

external interface EI

external fun createJsObjectAsExternalInterface(): EI
external fun getObjectValueEI(x: EI): String

fun box(): String {
    // Strings
    if (!isTestString("Test string")) return "Fail !isTestString"
    if (isTestString("Test string 2")) return "Fail isTestString"
    if (getTestString() != "Test string") return "Fail getTestString"
    if (concatStrings("A", "B") != "AB") return "Fail concatStrings 1"
    if (concatStrings("Привет ", "😀\uD83D") != "Привет 😀\uD83D") return "Fail concatStrings 2"
    if (concatStringsNullable("A", "B") != "AB") return "Fail concatStringsNullable 1"

    // Boolean
    if (!isTrueBoolean(true)) return "Fail !isTrueBoolean"
    if (isTrueBoolean(false)) return "Fail isTrueBoolean"
    if (!isFalseBoolean(false)) return "Fail !isFalseBoolean"
    if (isFalseBoolean(true)) return "Fail isFalseBoolean"
    if (getTrueBoolean() != true) return "Fail getTrueBoolean"
    if (getFalseBoolean() != false) return "Fail getFalseBoolean"

    // External interface
    val objAsEI: EI = createJsObjectAsExternalInterface()
    if (getObjectValueEI(objAsEI) != "object created by createJsObjectAsExternalInterface")
        return "Fail createJsObjectAsExternalInterface + getObjectValueEI"

    return "OK"
}