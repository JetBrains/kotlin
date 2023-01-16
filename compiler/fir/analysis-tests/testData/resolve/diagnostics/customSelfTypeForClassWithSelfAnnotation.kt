// FIR_DISABLE_LAZY_RESOLVE_CHECKS
@Self
class ClassWithCustomSelfTypeParameter<<!SELF_TYPE_PARAMETER_FOR_CLASS_WITH_SELF_TYPE!>Self<!>> {
    fun returnType(): Self {
        return <!RETURN_TYPE_MISMATCH!>this as Self<!>
    }
}