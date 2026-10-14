// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// SAM_CONVERSIONS: CLASS
// CHECK_BYTECODE_TEXT

// MODULE: lib
// FILE: JavaValBox.java
public interface JavaValBox {
    Val get();
}

// FILE: lib.kt
value class Val(val a: Int, val b: Int)

interface ValBox {
    fun get(): Val
}

// The anonymous object captures `v: Val`, so `lib`'s codegen emits `LoadableDescriptors : LVal;` on `MakeBoxKt$makeBox$1`. As the
// body of an inline function, this object is *regenerated* at every cross-module call site (see `box.kt`): the bytecode inliner
// re-reads the compiled class and re-emits it through a fresh constant pool. That round-trip must carry the attribute across
// correctly — copying its raw bytes verbatim would leave the descriptor indices pointing into `lib`'s pool (a `ClassFormatError`
// at load time), while dropping it would lose the JEP 401 preload hint. This regeneration goes through `AnonymousObjectTransformer`.
inline fun makeBox(v: Val): ValBox = object : ValBox {
    override fun get(): Val = v
}

// SAM conversion of a function value to a Java SAM interface (forced to a class by `SAM_CONVERSIONS: CLASS`), inside an inline
// function. At the cross-module call site the SAM wrapper is regenerated through `SamWrapperTransformer`, the third read-and-re-emit
// path that must pass `LOADABLE_DESCRIPTORS_ATTRIBUTE_PROTOTYPES` to `ClassReader.accept`. The wrapper's `get()` returns `Val`, so it
// carries `LoadableDescriptors : LVal;` too, which the SAM-wrapper regeneration must round-trip without a `ClassFormatError`.
inline fun makeSamBox(noinline supplier: () -> Val): JavaValBox = JavaValBox(supplier)

// MODULE: main(lib)
// FILE: box.kt
fun box(): String {
    val b = makeBox(Val(1, 2))
    val v = b.get()
    if (v != Val(1, 2)) return "FAIL: $v"

    val sb = makeSamBox { Val(3, 4) }
    val sv = sb.get()
    if (sv != Val(3, 4)) return "FAIL sam: $sv"

    return "OK"
}

// The regenerated inlined object and SAM wrapper in `main` must still carry their `LoadableDescriptors` attributes (for the captured
// `Val` field and the `get(): Val` signatures). With the raw-byte copy box() would fail to load them (dangling constant-pool index);
// with the attributes stripped only `BoxKt` would keep one (for the `Val` returned by the lambda passed to `makeSamBox`). The proper
// round-trip keeps all three, with the descriptors re-interned into `main`'s constant pool.
// 3 ATTRIBUTE LoadableDescriptors
// 3 ATTRIBUTE LoadableDescriptors : LVal;\n
