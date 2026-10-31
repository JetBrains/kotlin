# `EnumUsageLowering`

Rewrites the remaining uses of enums into calls that the lowered enum class (see [EnumClassLowering](EnumClassLowering.md))
provides. An entry access `E.X` becomes `E.$getEnumAt(getterId)`. The enum intrinsics behind the stdlib's `enumValues<T>()`,
`enumValueOf<T>(name)` and `enumEntries<T>()` become calls to `T`'s static `values()`, `valueOf()` and `entries` getter.

| | |
|---|---|
| Source | [EnumClassLowering.kt](../../../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/EnumClassLowering.kt) (`EnumUsageLowering`) |
| Stage | second stage, after inlining (`getLoweringsAfterInlining`): `NativeEnumWhenLowering` → `EnumClassLowering` → **`EnumUsageLowering`** → `VarargInjectionLowering` |
| Runs | always |
| Kind | file lowering (an `IrTransformer` that carries an `IrBuilderWithScope` for the enclosing declaration), Native-specific |
| Prerequisites | `EnumClassLowering` |

## What it does

- **`IrGetEnumValue`** (`E.X`) becomes `E.$getEnumAt(getterId)`. The function comes from `EnumsSupport.getValueGetter(E)`.
  `getterId` comes from `enumEntriesMap(E)` and is the entry's position in **name-sorted** order, not its ordinal.
- **Intrinsic calls**, recognized by `tryGetIntrinsicType` on these `@TypedIntrinsic` externals:

  | Intrinsic | Declared as | Becomes |
  |---|---|---|
  | `ENUM_VALUES` | `kotlin.native.internal.enumValuesIntrinsic<T>()` | `T.values()` |
  | `ENUM_VALUE_OF` | `kotlin.native.internal.enumValueOfIntrinsic<T>(name)` | `T.valueOf(name)` |
  | `ENUM_ENTRIES` | `kotlin.enums.enumEntriesIntrinsic<T>()` | `T.entries`, or `createEnumEntries(T.values())` for enums from old klibs without a static `entries` property |

  The public `enumValues`, `enumValueOf` and `enumEntries` are reified inline functions that call these intrinsics. After
  inlining, `T` is the concrete enum at the call site.
- If `T` is not a class (a type parameter) or is erased to `kotlin.Enum`, the call becomes `throwIllegalStateException()`.

## Example

From a `-Xphases-to-dump-before/after=EnumUsageLowering` dump. Stdlib bodies are shown after inlining.

```kotlin
// source
enum class Color { RED, GREEN, BLUE }
inline fun <reified T : Enum<T>> names() = enumValues<T>().map { it.name }

fun main() {
    val c = Color.BLUE
    println(enumValueOf<Color>("GREEN"))
    println(enumEntries<Color>().size)
    println(names<Color>())
}
```

```kotlin
// before                                         // after
val c = Color.BLUE                                val c = Color.`$getEnumAt`(getterId = 0)   // BLUE is first by name
return enumValueOfIntrinsic<Color>(name)          return Color.valueOf(name)
return enumEntriesIntrinsic<Color>()              return Color.entries                       // <get-entries>()
return enumValuesIntrinsic<Color>()               return Color.values()                      // inside the inlined names<Color>()
```

## Gotchas and implicit contracts

- **Two places must emit the same code.** Codegen's `CodeGenerator.getEnumEntry` emits the same `$getEnumAt(getterId)` call for
  enum entries it needs directly. Its KDoc says so explicitly. Its callers are `CAdapterCodegen` (C export) and
  `ObjCExportCodeGenerator`. If the access scheme changes, both must change.
- **`getterId` is name order.** Callers depend on `$VALUES` being sorted by name, which `EnumClassLowering` guarantees, so
  inserting or renaming an entry changes the ids at every call site. Inferred: callers in caches compiled against an older enum
  must be rebuilt. Unlike `NativeEnumWhenLowering`, this lowering does not call `dependenciesTracker.add`.
- **Works for enums this compilation does not lower.** `getValueGetter` creates the `$getEnumAt` symbol on demand through an
  `irAttribute`, and `enumEntriesMap` is computed from the declarations if not cached yet. So an access to an enum from a
  library or another fragment produces a call to its `$getEnumAt` by name.
  - Inferred: `$getEnumAt` is therefore part of the cross-module ABI of enums.
  - For enums in the same file, `EnumClassLowering` has already cached the map; it must be cached before the entries are
    removed.
- **Must run after inlining and after `EnumClassLowering`.**
  - The intrinsics only have a concrete `T` after the reified inline functions are inlined.
  - The static `values()`/`valueOf()` are found with `single { name == … && dispatchReceiverParameter == null }`, so they must
    exist.
  - `IntrinsicGenerator` rejects `ENUM_VALUES`, `ENUM_VALUE_OF` and `ENUM_ENTRIES` if they reach codegen
    (`reportNonLoweredIntrinsic`).
- **The old-klib fallback allocates.** Without an `entries` property, every `enumEntries<T>()` call creates a new
  `EnumEntries` list from a fresh `values()` copy, rather than reading the cached `$ENTRIES`.
- **`require(irClass.kind == ClassKind.ENUM_CLASS)`.** A class type argument that is not an enum fails the compilation.

## Tests

- `compiler/testData/codegen/box/enum/`, for example `emptyEnumValuesValueOf.kt` and the `enumEntries*` cases.
- `compiler/testData/codegen/box/enum/enumEntriesMultimoduleNoMappings.kt` (`entries` across modules).

## Related

- [EnumClassLowering](EnumClassLowering.md) defines `$VALUES`, `$getEnumAt` and the static members this lowering calls.
- `NativeEnumWhenLowering` lowers `when` over enums to ordinals before both.
- [intrinsics.md](../intrinsics.md) covers `@TypedIntrinsic` and how unlowered intrinsics are reported.
