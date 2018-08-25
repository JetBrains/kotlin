// WITH_RUNTIME
class Out<out T>

class Final
open class Open

@JvmField
val NO_WILDCARDS: Out<Open> = Out()
// field: FieldsKt::NO_WILDCARDS
// generic signature: LOut<LOpen;>;

@JvmField
var HAS_WILDCARDS: Out<Open> = Out()
// field: FieldsKt::HAS_WILDCARDS
// generic signature: LOut<+LOpen;>;

@JvmField
var NO_WILDCARDS_VAR: Out<Final> = Out()
// field: FieldsKt::NO_WILDCARDS_VAR
// generic signature: LOut<LFinal;>;

@JvmSuppressWildcards
@JvmField
var SUPPRESSED: Out<Open> = Out()
// field: FieldsKt::SUPPRESSED
// generic signature: LOut<LOpen;>;
