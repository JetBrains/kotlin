# `EnumClassLowering`

Turns an enum class into an ordinary class with static storage. The `IrEnumEntry` declarations are removed. A private static
`$VALUES` array holds the entry instances, and a `$ENTRIES` field holds the `EnumEntries` list. The synthetic `values()`,
`valueOf()` and `entries` members get real bodies, and a `$getEnumAt(getterId)` function gives indexed access to an entry.
Entry accesses (`IrGetEnumValue`) are rewritten to `$getEnumAt` calls by the next lowering, `EnumUsageLowering`.

| | |
|---|---|
| Source | [EnumClassLowering.kt](../../../../kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/EnumClassLowering.kt) (`EnumClassLowering`, with `EnumsSupport.getValueGetter`) |
| Stage | second stage, after inlining (`getLoweringsAfterInlining`): `StaticCallableReferenceOptimization` → `NativeEnumWhenLowering` → **`EnumClassLowering`** → `EnumUsageLowering` |
| Runs | always |
| Kind | file lowering, Native-specific |
| Prerequisites | `EnumWhenLowering` (satisfied by its subclass `NativeEnumWhenLowering`) |

## What it does

For every class of kind `ENUM_CLASS` (nested enums first, because `visitClass` transforms children before the class itself):

1. **Removes the entries.** Each `IrEnumEntry` is replaced by its `correspondingClass`, which becomes an ordinary nested class
   (`Color$BLUE` below). An entry without a body leaves nothing behind.
2. **Adds `$VALUES`.** A private, static, final `Array<E>` field (origin `DECLARATION_ORIGIN_ENUM`) at index 0 of the class. Its
   initializer:
   - allocates every entry with `createUninitializedInstance<EntryClass>()`, placed in **name order**;
   - stores the array into `$VALUES` (origin `FIELD_PRE_INIT`);
   - runs each entry's constructor call through `initInstance(array[getterId], ctorCall)`, in **declaration order**;
   - returns the array.
3. **Adds `$ENTRIES`.** A private, static, final `EnumEntries<E>` field, initialized with `enumEntries(arrayOf(...))` built from
   `$VALUES` in ordinal order.
4. **Gives the synthetic members real bodies:**
   - `values()` returns `valuesForEnum($VALUES)`;
   - `valueOf(name)` returns `valueOfForEnum(name, $VALUES)`;
   - the `entries` getter returns `$ENTRIES`.
5. **Adds `$getEnumAt(getterId: Int): E`**, which returns `$VALUES.<getWithoutBoundCheck>(getterId)`. It is created through
   `EnumsSupport.getValueGetter`, so callers in other files can obtain the same symbol, and added at the end of the class.

Each entry is described by `enumEntriesMap(enumClass)` in `backend.common`'s `EnumWhenLowering.kt`:
- `ordinal` is the declaration index;
- `getterId` is the index in **name-sorted** order.

## Example

Abridged from a `-Xphases-to-dump-before/after=EnumClassLowering` dump. Constructor calls already carry `name` and `ordinal`
from `EnumConstructorsLowering`, and default arguments from `NativeDefaultParameterInjector`.

```kotlin
// before
enum class Color(val rgb: Int, val label: String = "") {
    RED(0xff0000),
    GREEN(label = "g", rgb = 0x00ff00),
    BLUE(0x0000ff) { override fun describe() = "blue!" };
    open fun describe() = name
    companion object { val first = values().first() }
}
```

```kotlin
// after
class Color(...) : Enum<Color> {
    private static final val `$VALUES`: Array<Color> = {
        // name order: BLUE = 0, GREEN = 1, RED = 2
        val tmp0 = arrayOf(createUninitializedInstance<Color$BLUE>(),
                           createUninitializedInstance<Color>(),
                           createUninitializedInstance<Color>())
        #$VALUES = tmp0                                                        // published before any constructor runs
        initInstance(tmp0.getWithoutBoundCheck(2), Color("RED", 0, 0xff0000, /* default */))   // declaration order
        initInstance(tmp0.getWithoutBoundCheck(1), Color("GREEN", 1, 0x00ff00, "g"))
        initInstance(tmp0.getWithoutBoundCheck(0), Color$BLUE("BLUE", 2))
        tmp0
    }
    private static final val `$ENTRIES`: EnumEntries<Color> = {
        val tmp0 = #$VALUES
        enumEntries(arrayOf(tmp0.getWithoutBoundCheck(2), tmp0.getWithoutBoundCheck(1), tmp0.getWithoutBoundCheck(0)))  // ordinal order
    }

    class `Color$BLUE` : Color { override fun describe() = "blue!" }          // was the entry's body

    static fun values(): Array<Color> = valuesForEnum(#$VALUES)
    static fun valueOf(value: String): Color = valueOfForEnum(value, #$VALUES)
    static val entries: EnumEntries<Color> get() = #$ENTRIES
    static fun `$getEnumAt`(getterId: Int): Color = #$VALUES.getWithoutBoundCheck(getterId)
    ...
}
```

The program prints `65280`, `[RED, GREEN, blue!]`, `RED`. Iteration order and `values()` are still by ordinal.

## How it works

`EnumClassTransformer(irClass).run()` does four steps in order:
1. `transformEnumBody` collects the entries, replaces them with their entry classes, and fills the synthetic bodies.
2. `defineEntriesField` inserts `$ENTRIES` at index 0.
3. `defineValuesField` inserts `$VALUES` at index 0, so it ends up first and `$ENTRIES` second.
4. `defineValueGetter` adds `$getEnumAt`.

The code comment explains the order: both fields go to the beginning "to be initialized before companion object", and `$VALUES`
must be initialized before `$ENTRIES`, which reads it.

The entry constructor call is taken from `initializerExpression`. It is either a plain `IrConstructorCall` or, when named
arguments needed temporaries, an `ARGUMENTS_REORDERING_FOR_CALL` block whose last statement is the call. In the second case the
block is kept, its last statement is replaced by the `initInstance` call, and the block type becomes `Unit`. Anything else is
`error("Unexpected initializer: …")`.

## Gotchas and implicit contracts

- **`$VALUES` must stay sorted by name.** `valueOfForEnum` (`RuntimeUtils.kt`) binary-searches it with `String.compareTo`. The
  compile-time order comes from `Name.compareTo` (Java `String.compareTo`) in `enumEntriesMap`, and the two orders must agree.
  `valuesForEnum` copies the array into ordinal order on every call, so `values()` allocates.
- **`getterId` is baked into callers.**
  - `EnumUsageLowering` and codegen's `CodeGenerator.getEnumEntry` both emit `$getEnumAt(getterId)`.
  - `NativeEnumWhenLowering` bakes `ordinal`, and records a dependency for it (`dependenciesTracker.add(entry, weak = false)`).
  - Inferred: adding or renaming an entry shifts `getterId`s, so callers compiled against an older version of the enum, for
    example in a cache, must be recompiled.
- **`enumEntriesMap` must be computed before the entries disappear.** It is cached in an `irAttribute` on first use. The
  transformer computes it eagerly in its constructor, before `transformEnumBody` removes the `IrEnumEntry` declarations. A
  first call after this lowering would compute an empty map, and callers using `!!` or `getValue` would fail.
- **Instances are published before construction.** An entry constructor or initializer that reads `Color.values()` or
  another entry while `$VALUES` is being initialized sees allocated but possibly not yet constructed instances, not a recursive
  initialization.
- **`FIELD_PRE_INIT` has no reader.** The origin is `ObjectClassLowering.IrStatementOriginFieldPreInit`, described there as a
  hack to avoid early freezing; nothing in this checkout checks it.
- **The static fields are initialized by the next steps.** `$VALUES` and `$ENTRIES` are static fields with initializers, so
  [`StaticInitializersLowering`](StaticInitializersLowering.md) moves them into the class's lazy `$init_global`, and the static
  `values()`/`valueOf()`/`entries`/`$getEnumAt` functions get the initializer call.
- **Needs `<getWithoutBoundCheck>`.** It is looked up with `single { … }` on `Array` and added by
  `FunctionsWithoutBoundCheckGenerator` before the lowerings run. Without that phase the lowering fails while being constructed.

## Tests

- `compiler/testData/codegen/box/enum/`, for example `companionAccessingEnumValue.kt`, `enumCompanionInit.kt` and
  `emptyEnumValuesValueOf.kt` (initialization order and `valueOf`).
- `native/native.tests/testData/codegen/fileCheck/enumWithDifferentFileOrder.kt` and `enum_when.kt`.

## Related

- `EnumConstructorsLowering` adds the `name`/`ordinal` constructor parameters seen above.
- `NativeEnumWhenLowering` uses `ordinal` from the same `enumEntriesMap`.
- `EnumUsageLowering` rewrites entry accesses and the `enumValues`/`enumValueOf`/`enumEntries` intrinsics.
- [ObjectClassLowering](ObjectClassLowering.md) uses the same publish-before-construct pattern for objects.
- [StaticInitializersLowering](StaticInitializersLowering.md) makes `$VALUES`/`$ENTRIES` lazily initialized.
- [static-initializers.md](../static-initializers.md), section "Objects, companions, enums, instance initializers".
