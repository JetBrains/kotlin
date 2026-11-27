// RUN_PIPELINE_TILL: CODEGEN
// On Wasm, @JsName is going to be allowed only on properties.
// Any @JsName on a property accessor, written directly or with the `get:`/`set:` use-site target, will be prohibited.
package foo

external class A {
    // Only on the property: stays allowed
    @JsName("a_") val a: Int

    @property:JsName("b_") var b: Int

    // Only on accessors
    var x: Int
        @JsName("get_x") get
        set

    var y: Int
        get
        @JsName("set_y") set

    var z: Int
        @JsName("get_z") get
        @JsName("set_z") set

    // On the property and its accessors
    @JsName("v_") val v: Int
        @JsName("get_v") get

    @JsName("m_") var m: Int
        @JsName("get_m") get
        @JsName("set_m") set

    // Use-site targets
    @property:JsName("p_") @get:JsName("getP_") val p: Int

    @get:JsName("getQ_") @set:JsName("setQ_") var q: Int
}

// Top level
@JsName("aa_") external val aa: Int

external var xx: Int
    @JsName("get_xx") get
    set

@JsName("vv_") external val vv: Int
    @JsName("get_vv") get

@JsName("mm_") external var mm: Int
    @JsName("get_mm") get
    @JsName("set_mm") set

@get:JsName("getQq_") external var qq: Int
