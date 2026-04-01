// RUN_PIPELINE_TILL: FRONTEND
lateinit var foo: String
<!NOTHING_TO_INLINE!>inline<!> fun isFooInitialized() = ::foo.<!LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION!>isInitialized<!>
