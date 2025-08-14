// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN_SINGLE_MODULE_MODE

//FILE: main.kt
fun throwSomeJsException(): Int = js("{ throw new TypeError('Test'); }")
fun throwSomeJsPrimitive(): Int = js("{ throw 'Test'; }")
fun throwNumberFromJs(): Int = js("{ throw 42; }")
fun throwNullFromJs(): Int = js("{ throw null; }")

@JsExport
fun runWithThrowJsPrimitive() {
    throwSomeJsPrimitive()
}

@JsExport
fun runWithThrowJsNumber() {
    throwNumberFromJs()
}

@JsExport
fun runWithThrowJsNull() {
    throwNullFromJs()
}

@JsExport
fun runWithThrowJsException() {
    throwSomeJsException()
}

@JsExport
fun catchAndRethrowJsPrimitiveAsJsException() {
    rethrown = false
    try {
        throwSomeJsPrimitive()
    } catch (e: JsException) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsNumberAsJsException() {
    rethrown = false
    try {
        throwNumberFromJs()
    } catch (e: JsException) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsNullAsJsException() {
    rethrown = false
    try {
        throwNullFromJs()
    } catch (e: JsException) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsPrimitiveAsThrowable() {
    rethrown = false
    try {
        throwSomeJsPrimitive()
    } catch (e: Throwable) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsNumberAsThrowable() {
    rethrown = false
    try {
        throwNumberFromJs()
    } catch (e: Throwable) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsNullAsThrowable() {
    rethrown = false
    try {
        throwNullFromJs()
    } catch (e: Throwable) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsExceptionAsJsException() {
    rethrown = false
    try {
        throwSomeJsException()
    } catch (e: JsException) {
        rethrown = true
        throw e
    }
}

@JsExport
fun catchAndRethrowJsExceptionAsThrowable() {
    rethrown = false
    try {
        throwSomeJsException()
    } catch (e: Throwable) {
        rethrown = true
        throw e
    }
}

@JsExport
fun finallyJsPrimitive() {
    rethrown = false
    try {
        throwSomeJsPrimitive()
    } finally {
        rethrown = true
    }
    rethrown = false
}

@JsExport
fun finallyJsNumber() {
    rethrown = false
    try {
        throwNumberFromJs()
    } finally {
        rethrown = true
    }
    rethrown = false
}

@JsExport
fun finallyJsNull() {
    rethrown = false
    try {
        throwNullFromJs()
    } finally {
        rethrown = true
    }
    rethrown = false
}

@JsExport
fun finallyJsException() {
    rethrown = false
    try {
        throwSomeJsException()
    } finally {
        rethrown = true
    }
    rethrown = false
}

var rethrown = false
@JsExport
fun getRethrown() = rethrown

fun box() = "OK"

// FILE: entry.mjs
import { 
        runWithThrowJsPrimitive,
        runWithThrowJsNumber,
        runWithThrowJsNull,
        runWithThrowJsException,
        catchAndRethrowJsPrimitiveAsJsException,
        catchAndRethrowJsNumberAsJsException,
        catchAndRethrowJsNullAsJsException,
        catchAndRethrowJsPrimitiveAsThrowable,
        catchAndRethrowJsNumberAsThrowable,
        catchAndRethrowJsNullAsThrowable,
        catchAndRethrowJsExceptionAsJsException,
        catchAndRethrowJsExceptionAsThrowable,
        finallyJsPrimitive,
        finallyJsNumber,
        finallyJsNull,
        finallyJsException,
        getRethrown,
    } from "./index.mjs"

let nothrow = ""
try {
    runWithThrowJsPrimitive()
    nothrow += "runWithThrowJsPrimitive;";
} catch(e) {
    const t = typeof e;
    if (t !== "string") {
        throw Error("Expected 'string', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
}

try {
    runWithThrowJsNumber()
    nothrow += "runWithThrowJsNumber;";
} catch(e) {
    const t = typeof e;
    if (t !== "number") {
        throw Error("Expected 'number', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
}

try {
    runWithThrowJsNull()
    nothrow += "runWithThrowJsNull;";
} catch(e) {
    if (e !== null) {
        throw Error("Expected 'null', but '" + e + "' was received");
    }
}

try {
    runWithThrowJsException()
    nothrow += "runWithThrowJsException;";
} catch(e) {
    if (!(e instanceof TypeError)) {
        throw Error("Expected TypeError, but '" + e.name +"' ('" + e?.constructor?.name + "') was received");
    }
}

try {
    catchAndRethrowJsPrimitiveAsJsException()
    nothrow += "catchAndRethrowJsPrimitiveAsJsException;";
} catch(e) {
    const t = typeof e;
    if (t !== "string") {
        throw Error("Expected 'string', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsPrimitiveAsJsException");
    }
}

try {
    catchAndRethrowJsNumberAsJsException()
    nothrow += "catchAndRethrowJsNumberAsJsException;";
} catch(e) {
    const t = typeof e;
    if (t !== "number") {
        throw Error("Expected 'number', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsNumberAsJsException");
    }
}

try {
    catchAndRethrowJsNullAsJsException()
    nothrow += "catchAndRethrowJsNullAsJsException;";
} catch(e) {
    if (e !== null) {
        throw Error("Expected 'null', but '" + e +"' was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsNullAsJsException");
    }
}

try {
    catchAndRethrowJsPrimitiveAsThrowable()
    nothrow += "catchAndRethrowJsPrimitiveAsThrowable;";
} catch(e) {
    const t = typeof e;
    if (t !== "string") {
        throw Error("Expected 'string', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsPrimitiveAsThrowable");
    }
}

try {
    catchAndRethrowJsNumberAsThrowable()
    nothrow += "catchAndRethrowJsNumberAsThrowable;";
} catch(e) {
    const t = typeof e;
    if (t !== "number") {
        throw Error("Expected 'number', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsNumberAsThrowable");
    }
}

try {
    catchAndRethrowJsNullAsThrowable()
    nothrow += "catchAndRethrowJsNullAsThrowable;";
} catch(e) {
    if (e !== null) {
        throw Error("Expected 'null', but '" + e +"' was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsNullAsThrowable");
    }
}

try {
    catchAndRethrowJsExceptionAsJsException()
    nothrow += "catchAndRethrowJsExceptionAsJsException;";
} catch(e) {
    if (!(e instanceof TypeError)) {
        throw Error("Expected TypeError, but '" + e.name +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsExceptionAsJsException");
    }
}

try {
    catchAndRethrowJsExceptionAsThrowable()
    nothrow += "catchAndRethrowJsExceptionAsThrowable;";
} catch(e) {
    if (!(e instanceof TypeError)) {
        throw Error("Expected TypeError, but '" + e.name +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in catchAndRethrowJsExceptionAsThrowable");
    }
}

try {
    finallyJsPrimitive()
    nothrow += "finallyJsPrimitive;";
} catch(e) {
    const t = typeof e;
    if (t !== "string") {
        throw Error("Expected 'string', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in finallyJsPrimitive");
    }
}

try {
    finallyJsNumber()
    nothrow += "finallyJsNumber;";
} catch(e) {
    const t = typeof e;
    if (t !== "number") {
        throw Error("Expected 'number', but '" + t +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in finallyJsNumber");
    }
}

try {
    finallyJsNull()
    nothrow += "finallyJsNull;";
} catch(e) {
    if (e !== null) {
        throw Error("Expected 'null', but '" + e +"' was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in finallyJsNull");
    }
}

try {
    finallyJsException()
    nothrow += "finallyJsException;";
} catch(e) {
    if (!(e instanceof TypeError)) {
        throw Error("Expected TypeError, but '" + e.name +"' ('" + e?.constructor?.name + "') was received");
    }
    if (!getRethrown()) {
        throw Error("It wasn't rethrown in finallyJsException");
    }
}

if (nothrow) throw Error("Unexpected successful call(s): " + nothrow);
