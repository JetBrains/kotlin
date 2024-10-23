Compiler backend - transforming IR into the target executable format.

Implementations of backends for specific Kotlin targets are located in `backend.*` modules, except for the Kotlin/Native backend, which is located under `/kotlin-native/backend.native`.

### IR
IR is a lower-level representation of kotlin code, used at the backend stage of compilation. 
Despite this, it still has similar structure to the source code. 

For example:

```kotlin
fun abs(num: Int): Int {
    return if (num >= 0) num
    else -num
}
```
may be represented in IR as (simplified textual representation):
```
IrSimpleFunction name:abs visibility:public modality:FINAL returnType:kotlin.Int
  IrValueParameter name:num index:0 type:kotlin.Int
  IrBlockBody
    IrReturn type:kotlin.Int
      IrWhen type:kotlin.Int origin:IF
        IrBranch
          condition: IrCall symbol:'fun greater (arg0: kotlin.Int, arg1: kotlin.Int): kotlin.Boolean'
            arg0: IrGetValue symbol:'num' type:kotlin.Int
            arg1: IrConst type:kotlin.Int value:0
          result: IrGetValue symbol:'num' type:kotlin.Int
        IrBranch
          condition: IrConst type:kotlin.Boolean value:true
          result: IrCall symbol:'fun minus (arg0: kotlin.Int): kotlin.Int'
            arg0: IrGetValue symbol:'num' type:kotlin.Int
```

The classes of all IR elements inherit from [IrElement](ir.tree/gen/org/jetbrains/kotlin/ir/IrElement.kt)
and most of them are generated from specification in [IrTree.kt](ir.tree/tree-generator/src/org/jetbrains/kotlin/ir/generator/IrTree.kt).

IR is used by all kotlin backends for:
- Lowering
    - This is, modifying representation of the code to make it closer to the form expected by the target backend. The goal here is mainly to make it more convenient to convert the IR to the target form (JVM bytecode / JS code / LLVM bitcode / WASM bytecode). The actual conversion happens in codegen phase.
- Optimization
    - Happens alongside lowering.
- Serialization for later use - see `./serialization.common/ReadMe.md`

##### Note:
- There is also another intermediate representation, [FIR](../../fir) (Frontend IR), which is closer to source code, and is used for resolution, type inference, and simple desugaring. Therefore, what we refer to as IR should in reality be called Backend IR (it isn't for historical reasons).