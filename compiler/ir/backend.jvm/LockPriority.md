# Priority of locks in parallel runs of the JVM IR backend

To avoid deadlocks in backend code, please make sure that a lock that comes earlier in the list
never gets acquired by a thread that already holds one of the locks that come later.

1. `JvmBackendContext.inlineMethodGenerationLock`. Used to prevent concurrent generation of inline functions.
1. Global `IrLock` instance. Used in `SymbolTable`, 
   `lazyVar` instances in `IrLazyDeclaration`, `Fir2IrLazyDeclaration` subclasses,
   `Fir2IrDeclarationStorage`.
1. - lock in `ClassBuilderAndSourceFileList.asBytes`.
   - lock on `methodNode` in `InlineCodegen.cloneMethodNode`.
   - Locks protecting `InlineCache` maps.