// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
class TypealiasSelf {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias Self = String

    fun returnType(): Self {
        return this as Self
    }

    fun returnTypealias(): TypealiasSelf.Self {
        return "typealias"
    }
}
