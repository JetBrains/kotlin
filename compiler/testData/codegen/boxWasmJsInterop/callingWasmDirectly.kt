// TARGET_BACKEND: WASM

/*
 Here we pass export of another Wasm module to our import directly without JS layer.
 This enables strict type check without JS conversons.
 For instance, recursion groups of function types must fully match.

(module
  (func (export "addTwo") (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add))
 */
@JsFun("""
(() => {
    let bytes = [0, 97, 115, 109, 1, 0, 0, 0, 1, 7, 1, 96, 2, 127, 127, 1, 127, 3,
        2, 1, 0, 7, 10, 1, 6, 97, 100, 100, 84, 119, 111, 0, 0, 10, 9, 1,
        7, 0, 32, 0, 32, 1, 106, 11, 0, 10, 4, 110, 97, 109, 101, 2, 3, 1, 0, 0];
    let buffer = (new Int8Array(bytes)).buffer;
    let module = new WebAssembly.Module(buffer);
    let instance = new WebAssembly.Instance(module);
    return instance.exports.addTwo;
})()
""")
external fun addTwo(a: Int, b: Int): Int

fun box(): String {
    if (addTwo(100, 200) != 300) return "Fail1"
    return "OK"
}