typedef int32_t LLVMAttributeSet;

// Return value type of LLVMGetFunctionAttr is enum LLVMAttribute;
// it is not a strict enum, but used as a set of enum values instead.
// Currently it is not possible to configure interop to treat a enum in such way,
// so redeclare the function with appropriate return value type:
static inline LLVMAttributeSet LLVMGetFunctionAttrSet(LLVMValueRef Fn) {
    return LLVMGetFunctionAttr(Fn);
}