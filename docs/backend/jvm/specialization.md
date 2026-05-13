# Overview - function specialization via runtime monomorphization

The main rationale behind this experimental feature is performance optimization - an optimized copy of a specialized function is generated on the first call for each combination of specialized type parameters. Unboxed representations of type arguments are used in the function signature and to store local variables.

A function’s type parameter may be annotated with @JvmSpecialize. Such a function must be final and non-override. Annotated type parameters may be marked as reified, allowing for reified type operations, like in inline functions.

Some of the limitations of inline functions apply - a specialized function may not access private classes or call private inline functions. However, it is fine to access private and internal-non-@PublishApi properties and methods, since there are no ABI compatibility considerations - the bytecode of the specialized function is always in sync with its class.

# Affected compilation phases

## FIR Checkers

- Allow `reified` on specialized type parameters.
- Verify that only final non-override functions are specialized.

## IR Lowering

- Calls to specialized functions are replaced by `<jvm-indy>` intrinsics, and their arguments are wrapped in `<jvm-specialized-argument-marker>` markers. These markers are needed to more efficiently manipulate specialized arguments later.
- Read access of parameters and local variables of specialized types is wrapped in `<jvm-box-marker>` intrinsics.
- Write access of local variables of specialized types is wrapped in `<jvm-unbox-marker>` intrinsics.
- The value of the return expressions of specialized functions is wrapped in `<jvm-unbox-marker>` intrinsics.

## IR -> bytecode

Bytecode generation is mostly not affected. The intrinsics are emitted as `kotlin/jvm/internal/Intrinsics` calls, which will be eventually replaced or removed by later stages. Uninitialized local variables are assigned to `specializedTypeDefaultValueMarker()`.

## Inline reification

Specialized calls may refer to reified type parameters of inline functions the same way they refer to any other type. At this stage "inline type reifier" simply reifies specialized call metadata with reified type information.

## `AdjustSpecializedCallsMethodTransformer`

This method transformer adjusts the call-sites of specialized functions. In particular, it does the following:

- Replaces boxed types with unboxed representations in indy's signature.
- Unboxes arguments of specialized calls.
- Boxes results of specialized calls.
- Removes `<jvm-specialized-argument-marker>` markers.

## Redundant boxing optimization

Redundant boxing optimization is adjusted to work with boxing markers the same way it does with ordinary boxing methods. So unnecessary pairs of `<jvm-box-marker>` and `<jvm-unbox-marker>` are eliminated. Also the result of, `T -> box -> unbox -> T?` and `T? -> box -> unbox -> T` sequences are replaced with `coerce2NullableMarker` and `coerce2NonNullableMarker` intrinsics (because nullable representation is not always the boxed representation - case: inline value classes backed by non nullable non primitive types).

## `SpecializationTransformer`

This method transformer does two things:

- Tracks loads and stores of specialized values and inserts `specializedTypeMarker` markers for them.
- Tracks which local variable slots are ever used by specialized values and stores this information in the metadata.

# Bootstrap method

In runtime, the bootstrap method reads the bytecode of the specialized function, adjusts it in a linear pass, and then creates a new class containing only the optimized specialized function. The generated class uses the caller's package. In particular:

- `load` and `store` instructions' offsets are adjusted for long and double specialized values taking two slots.
- `aload`, `astore` and `areturn` instructions operating on specialized values are replaced with their primitive counterparts when necessary.
- `checkNotNullParameter` intrinsic is removed for arguments specialized to primitives.
- `<jvm-box-marker>`, `<jvm-unbox-marker>`, `coerce2NullableMarker` and `coerce2NonNullableMarker` are replaced with type-specific methods.
- Reified type operations markers are replaced with appropriate bytecode.
- Type arguments are substituted for nested specialized calls.
