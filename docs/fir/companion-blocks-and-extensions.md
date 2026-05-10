# Companion Blocks & Extensions FIR Design

This document discusses some design decisions for the implementation of the Companion Blocks & Extensions language feature in the K2
frontend.

See [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md) for the language design.

## Representation

### Companion Blocks

Companion blocks are a syntactical feature and have no direct representation in FIR. Companion block members are stored as members of the
containing `FirClass`.

The alternative to introduce an `FirCompanionBlock` and to store the members there was rejected for simplicity reasons as well as for
consistency with the Java representation. It also leaves the door open for a future evolution of the language feature where instead / in
addition to companion blocks, class members could be declared with the `companion` modifier.

During raw FIR building, the `isStatic` flag is set in their status and the `containingClassForStaticMemberAttr` attribute refers to the
containing class. This is exactly the representation previously used for static members from Java as well as for enum entries and for
special enum members
`values()`, `valueOf` and `entries`.

`FirResolvedStatus` also features an `isCompanion` flag. Using it instead of `isStatic` was rejected to keep consistency with Java statics.
Setting it in addition to `isStatic` didn't provide any additional benefits and therefore was also rejected.

To report some diagnostics on the companion block, a `KtSourceElement` refering to every companion block element is stored in the
`companionBlocks` declaration attribute.

### Companion Extensions

Companion extensions are represented as top-level callable declarations with the `isStatic` status flag set.

Note that regular top-level declarations (including regular extensions) don't have the `isStatic` status flag set regardless of the fact
that they are compiled to static declarations on the JVM.

The alternative to use the `isCompanion` flag was rejected for consistency with companion members.

The proposed interpretation of the `isStatic` status flag is that it's applied to a callable declaration invoked on a qualifier receiver. On
the other hand, the `isCompanion` status flag stays reserved for `companion object`s.

The receiver of a companion extension is represented using the `FirCallableDeclaration.receiverParameter: FirReceiverParameter`, just like
for regular extensions. While companion extension receivers are not allowed to have annotations or type arguments, they still need to be
resolved and nice errors need to be reported on them (as opposed to generic parser errors). Reusing the `FirReceiverParameter` is convenient
for this.

## FIR2IR

In the translation to IR, companion block members simply have `dispatchReceiverParameter` set to `null` which is the same mechanism as used
for representing Java statics.

Companion extensions have the same representation as regular top-level declarations. Their extension receiver is simply omitted in IR.

## Metadata

In metadata, both companion block members and extensions have the newly introduced `isStatic` bit set on their `flags`.

## Binary Poisoning

The compiler can read binaries with metadata one version forward.

Compiler version 2.4 implements checks during resolution to report errors when companion block members or extensions are called without the
LF enabled. For this reason, binaries with metadata version 2.5 and higher don't need to be poisoned.

Earlier compiler versions didn't contain these checks; therefore, binaries with metadata version 2.4 need to be poisoned so
that 2.3.X compilers don't miscompile calls to companion block members or extensions.
