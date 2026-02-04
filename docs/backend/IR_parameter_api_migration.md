# New IR parameter API + migration guide

## Summary

It has been refactored how value parameters in `IrFunction` and value arguments in `IrMemberAccessExpression` are represented ([KT-68003](https://youtrack.jetbrains.com/issue/KT-68003/Context-parameters-preparations)). The old API is deprecated and scheduled for removal somewhere around Kotlin 2.2.20 or 2.3. Here we provide a guide for compiler plugin authors how to migrate to the new API.

> Note: compiler plugin API is still experimental, we change it here and there quite regularly. Most of those changes used to be simple (such as moving code around) and should be easy to apply by "finding something similar". However, this change is big and broad enough that we decided to have a short deprecation cycle and publish a dedicated guide for migration (also used internally). It also describes how to properly take care of the upcoming [context parameters](https://github.com/Kotlin/KEEP/blob/context-parameters/proposals/context-parameters.md) in your IR plugins.

The old API is implemented on top of the new one, which is the source of truth. Any modification done via either API should be reflected in the other one. Old API preserves all its previous semantics, except some rare corner cases around receiver arguments.

## API change

* `IrFunction`:
  * Added property `var parameters: List<IrValueParameter>`  - **It stores ***all*** value parameters: dispatch receiver, extension receiver, context parameters and regular parameters.**
    * You can tell the kind of a parameter with new `IrValueParameter.kind` property.
  * Deprecated other parameter-related APIs: `valueParameters`, `dispatchReceiverParameter` (only setter),  `extensionReceiverParameter`, `contextReceiverParametersCount`.
    * The getter of `dispatchReceiverParameter` is now only a handy util that searches `parameters`.
    * There is also a new util for the opposite result: `nonDispatchParameters`.
    * (Because it's very common in backend to only need to handle dispatch or all-except-dispatch.)
* `IrValueParameter`:
  * Added property `var kind: IrParameterKind`, which can be either: `DispatchReceiver`, `ExtensionReceiver`, `Context`, `Regular`.
    * It is designed to distinguish parameters of functions. But there also are a few more occurrences of `IrValueParameter` in IR. For those we set the`kind` to:
      * `IrClass.thisReceiver`, `IrScript.thisReceiver` -\> `DispatchReceiver`.
      * Other `IrValueParameter`s in `IrScript` -\> `Regular`.
  * Added and immediately deprecated `var indexInOldValueParameters` property.
    * It is an index in the deprecated `valueParameters` list.
    * It is always equal to the deprecated `index` property, but has a clearer name as to what it refers to.
      * It may be helpful to replace all usages of `index` with `indexInOldValueParameters` at the beginning of migration.
  * Added property `var indexInParameters: Int` .
    * It is an index in **`parameters`** list.
    * **It may therefore be different than** `indexInOldValueParameters`, in case a function has any receiver parameter.
    * Both`indexInParameters` and `indexInOldValueParameters` are automatically updated when adding/removing a parameter via either old or new API.
    * Once we are able to remove the old `index`, the plan is to rename this property back to `index`.
  * This means that for now we have three indices. In summary:
    * Old API expects `indexInOldValueParameters`.
    * New API expects `indexInParameters`.
    * `index` - don't use.
* `IrMemberAccessExpression`
  * Added property `val arguments: MutableList<IrExpression?>` - **It stores ***all*** value arguments: dispatch receiver, extension receiver, context arguments and regular arguments.**
    * It should correspond 1 to 1 with `IrFunction.parameters`.
    * A single list means that now **you cannot tell the kind of the argument (dispatch/extension/regular) by the call-site itself.**  If you need this information, you have to reach out to the corresponding parameter, e.g. via usual `call.arguments zip call.symbol.owner.parameters`.
  * Deprecated other argument-related APIs: `getValueArgument`, `putValueArgument`, `valueArgumentsCount`, `extensionReceiver`.
    * `dispatchReceiver` is not deprecated, but discouraged in most cases - please see its kdoc.
    * In analogy to `IrFunction`, there is also a new `nonDispatchArguments` util. Same as `dispatchReceiver`, it should be used with care.
* `IrCallableReference`
  * `IrFunctionReference`, `IrFunctionExpression` and `IrPropertyReference` are considered deprecated.
  * They are going to be replaced by `IrRichFunctionReference` and `IrRichPropertyReference`.
  * However, as of kotlin 2.2.0, not all compiler code was migrated to the new "rich" references yet. More importantly, **the new reference types will not appear in the compiler plugin code yet**. (Currently, there is a transformation from old to new ones later in the compilation process, see `UpgradeCallableReferences`.) But the plan is to fully migrate to them eventually, so for now, we encourage to handle both new and old callable reference types. 
  * The difference between those two reference types is a bit more complex, please see the kdoc of `IrRichFunctionReference`.
 
## Motivation

### 1\. Ensure proper order in the presence of context parameters

To avoid the common bug when we have code like:

```
function.dispatchReceiverParameter?.process()
function.extensionReceiverParameter?.process()
function.valueParameters.forEach { it.process() }
```

while `process` expects the same order of parameters as present in ABI. Because then it should be:

```
function.dispatchReceiverParameter?.process()
function.valueParameters.take(function.contextReceiverCount).forEach { it.process() }
function.extensionReceiverParameter?.process()
function.valueParameters.drop(function.contextReceiverCount).forEach { it.process() }
```

Now it simply becomes:

```
function.parameters.forEach { it.process() }
```

### 2\. Avoid not taking receiver parameters into account by accident

We often have a code like
`if (valueParameters.any { it.type.isLambda && it.isInlne }) { ... }`
which is OK, because in the language, receivers cannot be inline. But if we'd remove `isInline`, it likely becomes a bug because extension receiver can have a functional type. OOTH we check context parameters here, which also cannot be inline.

Another example: it's easy to miss `declaration.extensionReceiverParameter?.accept(this)` in visitors.

We should rather process all parameters/arguments by default. That way we may also future-proof additional language features for other parameter kinds, just in case. An example is bound context parameters in callable extension, see `3.`

### 3\. Support callable references with bound context parameters

So far only dispatch and extension receivers could be bound in a reference to function or property, but context parameters will also need to be bound. It will be easier to implement with unified `parameters`.

## What and how to migrate

(An example branch: `wlitewka/ir-parameters-migration-backend-jvm`)

1. Simply replace usages of the deprecated API with the new one (i.e. mostly with `parameters` and `arguments`).
   * Note: as mentioned `IrMemberAccessExpression.dispatchReceiver`  also counts, but it's not strictly deprecated - please see its KDoc.
2. When possible, refactor code handling callable references to support *any* parameter as either bound or unbound.
   * Currently, only dispatch receiver and extension receiver can be bound.
   * With context parameters feature, context parameters will always be bound when obtaining a callable reference.
   * Instead of relying on those two constraints, we want to liberally allow *all* kinds of parameters to be either bound or unbound in the backend, even if the language is not able to represent it (yet).
     * Because, as of writing this, binding context parameters is not implemented yet (it requires `IrRichCallableReference`), that refactoring is not fully testable - we still only test binding dispatch receiver or extension receiver. So for now treat it as best-effort.
   * The exception is kotlin reflection (mainly `KCallable`), which will still only support binding dispatch and extension receivers. We may be able to implement binding contexts in the future.
3. Try to uniformly handle all kinds of parameters/arguments, even if the previous code didn't. Only filter out a specific `IrParameterKind` when it is required.
   * Most likely for historical reasons, the current code oftentimes only processes `valueParameters` (or only arguments returned by `getValueArgument()`). This means that it sees `Regular` and `Context` parameters/arguments, and ignores `DispatchReceiver` or `ExtensionReceiver`. Rarely is it the most appropriate combination. The migrate code should instead:
     * If it is interested in `Regular` parameters or arguments, then, in almost all cases, it should also handle `ExtensionReceiver` and `Context` , because from the backend perspective, both of them are mostly just a synthetic sugar for rather "normal" parameters.
     * However, `DispatchReceiver` quite often requires special handling. It should be decided case by case whether to use `parameters` or `nonDispatchParameters`. Still, `parameters` are preferable.
   * Example: given code like
     `function.valueParameter.any { it.isInline }`
     just replace `valueParameters` with `parameters`. Currently, kotlin only permits `inline` on regular parameters, but it should not hurt to consider all of them.
   * Note, `IrConstructor` is particularly interesting, because:
     * It can never have `ExtensionReceiver` or `Context` parameters. It could possibly have `Context` in the future, when the context parameter design expands to constructors or classes, but for now we assume those should be processed the same way as `Regular`.
     * It may have `DispatchReceiver`, in case it is a constructor of `inner class`. That parameter is then turned into `Regular` at some point during lowering.
     * Therefore, it's always safe to replace `valueParameters` with `nonDispatchParameters`. But in most cases it should be replaced with `parameters` instead.
4. If you need to check a kind of an *argument,* you have to reach out to the corresponding parameter.
   * Example: `val contextArguments = (call.arguments zip call.symbol.owner.parameters).filter { it.second.kind == IrParameterKind.Context }`
   * The above won't work if you have `IrCallableReference` or `IrProperty`. In that case, use `getAllArgumentsWithIr()`.
   * There are also shorthands specifically for dispatch receiver: `function.dispatchReceiverParameter`, `function.nonDispatchParameters`, `call.dispatchReceiver`, `call.nonDispatchArguments` .
5. Ensure that `arguments` of a call matches 1-to-1 with `parameters` of a callee. In other words, `arguments.size == parameters.size`,.
   * We of course used to uphold that for `valueParameters` before. But now it also extends to receiver parameters, and it wasn't *always* perfect. Occasionally we could have a call that had `dispatchReceiver != null`, while on callee `dispatchReceiverParameter == null` (or vice versa), at least for *some* time in the compilation pipeline. Think `@JvmStatic`, inner class's constructor, members of `IrScript` - tricky cases like that. It used to work somehow, but because now receivers are in the list, such inconsistency can cause index mismatch for subsequent parameters/arguments.
   * One peculiarity is that in old API, `call.dispatchReceiver == null` could either mean "there is no dispatch argument" or "dispatch argument is (temporarily) `null`". In new API, the former is `arguments = []`, the latter `arguments = [null]`.
6. About indexing `arguments`:
   * To fill out arguments of some call, we used to have a code like this:

     ```
     call.extensionReceiver = ...
     call.putValueArgument(0, ...)
     call.putValueArgument(1, ...)
     ```
   * We assumed we don't really need `dispatch/extensionReceiver`, because:
     * When you know exactly what you're calling (e.g. some builtin) - then you should know its ABI shape, you can just hardcode all indices.
       * I.e.:

         ```
         call.arguments[0] = ...
         call.arguments[1] = ...
         call.arguments[2] = ...
         ```
       * In case you have `IrValueParameter` at hand, you can also write `call.arguments[param]` instead of `call.arguments[param.indexInParameter]`. It would be preferred, actually.
       * Specifically for dispatch receiver - when you know the callee has one, it has to be the first parameter - it is OK to just hardcode `arguments[0]`.
     * When the callee is unknown - then in *most cases* you likely don't care what kind of argument it has. If you do, you have to `arguments zip parameters`, or similar.
       * In particular, when *changing* kinds of parameters (e.g. dispatch to regular, extension to regular), you don't need to adjust calls anymore, so long as indices stay the same.
     * Tip: you also copy/assign arguments with `call.arguments.assignFrom(irExpressionList)`.
7. Quite often parameters are used in a check for a particular known function (usually from stdlib), or otherwise some known function shape. In those cases, instead of filtering specific parameters, please try out `IrFunction.hasShape()`.
   * Examples:
     * ```
       val isAnyEquals = 
         function.name == StandardNames.EQUALS_NAME &&
         function.dispatchReceiverParameter != null &&
         function.extensionReceiverParameter == null &&
         function.valueParameters.size == 1 &&
         function.valueParameters[0].type == irBuiltIns.anyNType
       ```

       into

       ```
       val isEqualsOnAny = 
         function.name == StandardNames.EQUALS_NAME &&
         function.hasShape(
           dispatchReceiver = true,
           regularParameters = 1,
           parameterTypes = listOf(null, irBuiltIns.anyNType)
         )
       ```
     * `function.valueParamters.isEmpty()`
       into
       `function.hasShape(regularParameters = 0)`
     * `function.valueParamters.singleOrNull()?.type?.isInt() == true`
       into
       `function.hasShape(regularParameters = 1, parameterTypes = listOf(irBuiltIns.intType))`
     * `function.valueParamters.singleOrNull()?.type?.isPrimitiveType() == true`
       into
       `function.hasShape(regularParameters = 1) && function.parameters[0].type.isPrimitiveType()`
