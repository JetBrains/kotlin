// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@MustBeDocumented
annotation class DocAnn

annotation class NotDocAnn

@DocAnn class My

@NotDocAnn class Your
