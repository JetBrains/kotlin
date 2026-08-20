// All top-level properties of a file are initialized together on the first access to any of them.
// The first initializer below throws an IrLinkageError, the file is then marked as failed to initialize;
// subsequent reads of any top-level property of this file throw NoClassDefFoundError("Could not initialize file").
val removedCompanionFunRef = B::removedCompanionFun
val removedCompanionValRef = B::removedCompanionVal
val removedCompanionVarRef = B::removedCompanionVar
