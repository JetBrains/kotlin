# Basic step-by-step inlining example for proposed model

Let's check the following code:
```kotlin
// lib
fun process(x: Int) { /* some code here */ }
inline fun run(block: () -> Int) = process(block())
// main
fun foo() {
    run { 42 }
}
```

We would dig deeper into the compilation of the main module, assuming lib is already compiled.


After frontend execution, we get something like that. Here we have all calls resolved.
Functions from dependencies (lib) are loaded as LazyIr.

```kotlin
fun foo() : Unit {
    run(
        lambda@{ return@lambda 42 }
    )
}

// dependencies: 
// lazyIR without bodies
fun process(x: Int): Unit
inline fun run(block: Function0<Int>)
// ... lazyIR for Int, Unit, Function and so on 
```

Then, pre-inline lowering happens. But as we have a very simple example, there is nothing to do.

Next, we need to load Ir of run function. For that we need to run Deserializer. But we can't run linker,
so references inside function body wouldn't be resolved.

```kotlin
//  IrFunction
//      name = run
//      isInline = true
//      valueParameter0 
//        irType
//           classifier = Lazy class kotlin.Function0 (from Lazy run function)
//           typeArgument0 = Lazy class kotlin.Int (from Lazy run function)
//      returnType = Lazy class kotlin.Unit (from Lazy run function)
//      body
//           IrCall symbol = Unbound function symbol with signature "process(Int) : Unit"
//                returnType = IrType classifier = Unbound class symbol with singnature kotlin.Unit 
//                valueArgument0 = 
//                    IrCall symbol = Unbound function symbol with signature Function0.invoke() 
//                         typeArgument0 = IrType classifier = Unbound class symbol with singnature kotlin.Int
//                         valueArguemnt0 = IrGet valueParamenter block
//                         type = Unbound class symbol with singnature kotlin.Int 
//
inline fun run(block: Function0<Int>) : Unit { // note, that here we merged LazyIr of run function with deserialized body
    process(block.invoke())
}
```

Now this function can be inlined to original.

```kotlin
// IrFunction 
//   name = foo
//   returnType = Lazy class kotlin.Unit
//   body
//       IrReturnableBlock symbol=symbol1
//         type = Lazy class kotlin.Unit
//         IrInlinedFunctionBlock required for debug information
//            IrReturn 
//               target=symbol1
//               value = IrCall 
//                 symbol = Unbound function symbol with signature "process(Int) : Unit"
//                 returnType = IrType classifier = Unbound class symbol with singnature kotlin.Unit 
//                 valueArgument0 
//                    IrReturnableBlock symbol=symbol2
//                      type = Lazy class for kotlin.Int
//                      IrInlinedFunctionBlock required for debug information
//                         IrReturn target=symbol2 value = IrConst<Int>(42)  
fun foo() : Unit {
    inlinedBlock@{
        return@inlinedBlock process(lambda@ { return@lambda 42 })
    }
}
```

Several side notes on the result:
1. We have both lazy references to kotlin.Unit/kotlin.Int and unbound ones in the tree now. 
   It is fine, as we only need to deserialize it now.
2. While inlining we need to understand that we need special handling of
   `Unbound function symbol with signature Function0.invoke()`, this must be done by signature only. 
3. IrReturnableBlock is represented like it works now, while IrInlinedFunctionBlock is significantly simplified.
   Probably we need to redesign both to make them serializable.