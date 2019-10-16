inline fun f(x: Int = 1) = x

// MUST contain these instructions to avoid breaking the inliner.
// See `expandMaskConditionsAndUpdateVariableNodes`.
// 1 ILOAD 1\s*ICONST_1\s*IAND\s*IFEQ L1
// 1 ICONST_1\s*ISTORE 0\s*(L\d\s*)*L1
